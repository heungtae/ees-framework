package com.ees.cluster.raft;

import com.ees.cluster.lock.DefaultDistributedLockService;
import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.LockRecord;
import com.ees.cluster.raft.snapshot.ClusterSnapshotStore;
import com.ees.cluster.raft.snapshot.FileClusterSnapshotStore;
import com.ees.cluster.raft.snapshot.ClusterSnapshot;
import com.ees.cluster.state.ClusterStateRepository;
import com.ees.cluster.state.InMemoryClusterStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ees.cluster.assignment.AssignmentService.DEFAULT_AFFINITY_KIND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterStateMachineSnapshotTest {

    @TempDir
    Path tempDir;

    @Test
    void snapshotRoundTripRestoresAssignmentsAndLocks() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);
        ClusterStateRepository repository = new InMemoryClusterStateRepository(clock);
        RaftAssignmentService assignments = new RaftAssignmentService(repository, Duration.ofMinutes(5), clock);
        DistributedLockService locks = new DefaultDistributedLockService(repository, clock);
        ClusterSnapshotStore store = new FileClusterSnapshotStore(tempDir);
        String groupId = "group-" + UUID.randomUUID();

        ClusterStateMachine machine = new StaticGroupStateMachine(assignments, locks, store, groupId, clock);

        assignments.applyAssignments(groupId, List.of(
                new Assignment(groupId, 0, "node-1", List.of("eq-1"), null, 1L, clock.instant()))
        );
        assignments.assignKey(groupId, 0, DEFAULT_AFFINITY_KIND, "key-1", "app-1", KeyAssignmentSource.MANUAL);
        locks.tryAcquire("lock-1", "node-1", Duration.ofSeconds(30), Map.of("meta", "v"));

        machine.takeSnapshot();
        ClusterSnapshot snapshot = store.loadLatest(groupId).orElseThrow();

        ClusterStateRepository restoredRepo = new InMemoryClusterStateRepository(clock);
        RaftAssignmentService restoredAssignments = new RaftAssignmentService(restoredRepo, Duration.ofMinutes(5), clock);
        DistributedLockService restoredLocks = new DefaultDistributedLockService(restoredRepo, clock);
        ClusterStateMachine reloaded = new StaticGroupStateMachine(restoredAssignments, restoredLocks, store, groupId, clock);
        reloaded.loadSnapshot();

        Optional<Assignment> assignment = restoredAssignments.findAssignment(groupId, 0);
        assertTrue(assignment.isPresent());
        assertEquals("node-1", assignment.get().ownerNodeId());

        Optional<LockRecord> lock = restoredLocks.getLock("lock-1");
        assertTrue(lock.isPresent());
        assertEquals("node-1", lock.get().ownerNodeId());

        Map<Integer, Map<String, Map<String, com.ees.cluster.model.KeyAssignment>>> restoredKeys =
                restoredAssignments.snapshotKeyAssignments(groupId);
        assertTrue(restoredKeys.getOrDefault(0, Map.of()).containsKey(DEFAULT_AFFINITY_KIND),
                () -> "restored key assignments: " + restoredKeys);

        Optional<com.ees.cluster.model.KeyAssignment> keyAssignment =
                restoredAssignments.getKeyAssignment(groupId, 0, DEFAULT_AFFINITY_KIND, "key-1");
        assertTrue(keyAssignment.isPresent());
        assertEquals(DEFAULT_AFFINITY_KIND, keyAssignment.get().kind());
    }

    @Test
    void leaderGuardRespectsSafeModeAndLeadership() {
        RebalanceSafeModeGuard safeMode = new RebalanceSafeModeGuard();
        RaftStateMachineMetrics metrics = new RaftStateMachineMetrics();
        LeaderProcessingGuard guard = new LeaderProcessingGuard(new StubLeaderService("group-1", "node-1"),
                "group-1", "node-1", safeMode, metrics);

        ProcessingDecision allowed = guard.allowProcessing();
        assertTrue(allowed.allowed());

        LeaderProcessingGuard followerGuard = new LeaderProcessingGuard(new StubLeaderService("group-1", "node-1"),
                "group-1", "node-2", safeMode, metrics);
        ProcessingDecision deniedFollower = followerGuard.allowProcessing();
        assertFalse(deniedFollower.allowed());

        safeMode.enterSafeMode("rebalance");
        ProcessingDecision deniedSafeMode = guard.allowProcessing();
        assertFalse(deniedSafeMode.allowed());
        assertTrue(deniedSafeMode.reason().contains("safe-mode"));
    }

    private static final class StaticGroupStateMachine extends ClusterStateMachine {
        private final String groupId;

        StaticGroupStateMachine(RaftAssignmentService assignments,
                                DistributedLockService locks,
                                ClusterSnapshotStore store,
                                String groupId,
                                Clock clock) {
            super(assignments, locks, store, clock, 10L, 0L, new RaftStateMachineMetrics(), new RebalanceSafeModeGuard());
            this.groupId = groupId;
            metrics().setGroupId(groupId);
        }

        @Override
        protected String groupIdAsString() {
            return groupId;
        }
    }

    private static final class StubLeaderService implements com.ees.cluster.leader.LeaderElectionService {

        private final String groupId;
        private final String leaderId;

        private StubLeaderService(String groupId, String leaderId) {
            this.groupId = groupId;
            this.leaderId = leaderId;
        }

        @Override
        public java.util.Optional<com.ees.cluster.model.LeaderInfo> tryAcquireLeader(String groupId, String nodeId, com.ees.cluster.model.LeaderElectionMode mode, java.time.Duration leaseDuration) {
            return getLeader(groupId);
        }

        @Override
        public boolean release(String groupId, String nodeId) {
            return false;
        }

        @Override
        public java.util.Optional<com.ees.cluster.model.LeaderInfo> getLeader(String groupId) {
            if (!this.groupId.equals(groupId)) {
                return java.util.Optional.empty();
            }
            com.ees.cluster.model.LeaderInfo info = new com.ees.cluster.model.LeaderInfo(groupId, leaderId,
                    com.ees.cluster.model.LeaderElectionMode.RAFT, 1L, Instant.now(), Instant.now().plusSeconds(5));
            return java.util.Optional.of(info);
        }

        @Override
        public void watch(String groupId, java.util.function.Consumer<com.ees.cluster.model.LeaderInfo> consumer) {
        }
    }
}
