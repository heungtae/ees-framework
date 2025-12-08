package com.ees.cluster.raft;

import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.raft.RaftAssignmentService;
import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.raft.command.AssignKeyCommand;
import com.ees.cluster.raft.command.AssignPartitionCommand;
import com.ees.cluster.raft.command.CommandType;
import com.ees.cluster.raft.command.LockCommand;
import com.ees.cluster.raft.command.RaftCommandCodec;
import com.ees.cluster.raft.command.RaftCommandEnvelope;
import com.ees.cluster.raft.command.ReleaseLockCommand;
import com.ees.cluster.raft.command.RevokePartitionCommand;
import com.ees.cluster.raft.command.UnassignKeyCommand;
import com.ees.cluster.raft.snapshot.ClusterSnapshot;
import com.ees.cluster.raft.snapshot.ClusterSnapshotStore;
import com.ees.cluster.raft.snapshot.FileClusterSnapshotStore;
import com.ees.cluster.raft.snapshot.ClusterSnapshotStores;
import com.ees.cluster.raft.RaftServerConfig;
import com.ees.cluster.raft.RebalanceSafeModeGuard;
import com.ees.cluster.state.ClusterStateRepository;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Skeleton state machine that will apply Raft log entries to cluster services.
 */
public class ClusterStateMachine extends BaseStateMachine {

    private static final Logger log = LoggerFactory.getLogger(ClusterStateMachine.class);
    private static final long SNAPSHOT_VERSION = 1;

    private final RaftAssignmentService assignmentService;
    private final DistributedLockService lockService;
    private final Clock clock;
    private final ClusterSnapshotStore snapshotStore;
    private final long snapshotThreshold;
    private final AtomicLong appliedSinceSnapshot = new AtomicLong();
    private final RaftStateMachineMetrics metrics;
    private final RebalanceSafeModeGuard safeModeGuard;

    public ClusterStateMachine(RaftAssignmentService assignmentService, DistributedLockService lockService) {
        this(assignmentService, lockService, new FileClusterSnapshotStore(java.nio.file.Path.of("data/raft/snapshots")));
    }

    public ClusterStateMachine(RaftAssignmentService assignmentService,
                               DistributedLockService lockService,
                               ClusterSnapshotStore snapshotStore) {
        this(assignmentService, lockService, snapshotStore, Clock.systemUTC(), 1_000L);
    }

    public ClusterStateMachine(RaftAssignmentService assignmentService,
                               DistributedLockService lockService,
                               ClusterSnapshotStore snapshotStore,
                               Clock clock,
                               long snapshotThreshold) {
        this(assignmentService, lockService, snapshotStore, clock, snapshotThreshold, new RaftStateMachineMetrics(), new RebalanceSafeModeGuard());
    }

    public ClusterStateMachine(RaftAssignmentService assignmentService,
                               DistributedLockService lockService,
                               ClusterSnapshotStore snapshotStore,
                               Clock clock,
                               long snapshotThreshold,
                               RaftStateMachineMetrics metrics,
                               RebalanceSafeModeGuard safeModeGuard) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.lockService = Objects.requireNonNull(lockService, "lockService must not be null");
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.snapshotThreshold = snapshotThreshold;
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.safeModeGuard = Objects.requireNonNull(safeModeGuard, "safeModeGuard must not be null");
    }

    public static ClusterStateMachine forConfig(RaftAssignmentService assignmentService,
                                                DistributedLockService lockService,
                                                ClusterStateRepository repository,
                                                RaftServerConfig config) {
        ClusterSnapshotStore store = ClusterSnapshotStores.forConfig(config, repository);
        return new ClusterStateMachine(assignmentService, lockService, store, Clock.systemUTC(), config.getSnapshotThreshold(), new RaftStateMachineMetrics(), new RebalanceSafeModeGuard());
    }

    public RaftStateMachineMetrics metrics() {
        return metrics;
    }

    public RebalanceSafeModeGuard safeModeGuard() {
        return safeModeGuard;
    }

    @Override
    public void initialize(RaftServer server, RaftGroupId groupId, RaftStorage storage) throws IOException {
        super.initialize(server, groupId, storage);
        metrics.setGroupId(groupIdAsString());
        metrics.markStarted(clock.instant());
        metrics.setSafeMode(safeModeGuard.isSafeMode(), safeModeGuard.reason());
        loadSnapshot();
        log.info("Initialized ClusterStateMachine for group {}", groupId);
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        Objects.requireNonNull(trx, "transaction must not be null");
        byte[] data = extractLogData(trx);
        if (trx.getLogEntry() != null) {
            updateLastAppliedTermIndex(trx.getLogEntry().getTerm(), trx.getLogEntry().getIndex());
            metrics.recordApply(trx.getLogEntry().getIndex(), clock.instant());
        }
        if (data.length == 0) {
            return CompletableFuture.completedFuture(Message.valueOf("empty"));
        }
        try {
            RaftCommandEnvelope envelope = RaftCommandCodec.deserialize(data);
            return handleCommand(envelope)
                    .whenComplete((ignored, error) -> maybeTriggerSnapshot())
                    .thenApply(ignored -> Message.valueOf(envelope.type().name()))
                    .toCompletableFuture();
        } catch (Exception e) {
            log.error("Failed to apply raft command", e);
            return CompletableFuture.completedFuture(Message.valueOf("error:" + e.getClass().getSimpleName()));
        }
    }

    @Override
    public long takeSnapshot() throws IOException {
        TermIndex index = getLastAppliedTermIndex();
        long term = index != null ? index.getTerm() : -1L;
        long logIndex = index != null ? index.getIndex() : -1L;
        ClusterSnapshot snapshot = new ClusterSnapshot(
                SNAPSHOT_VERSION,
                groupIdAsString(),
                term,
                logIndex,
                clock.instant(),
                lockService.snapshotLocks(),
                assignmentService.snapshotAssignments(groupIdAsString()),
                assignmentService.snapshotKeyAssignments(groupIdAsString())
        );
        snapshotStore.persist(snapshot);
        metrics.recordSnapshot(logIndex, snapshot.takenAt());
        appliedSinceSnapshot.set(0L);
        log.info("Taking snapshot at term={}, index={} (takenAt={})", term, logIndex, snapshot.takenAt());
        return logIndex;
    }

    @Override
    public void reinitialize() throws IOException {
        super.reinitialize();
        metrics.markStarted(clock.instant());
        loadSnapshot();
        log.info("Reinitialized ClusterStateMachine for group {}", getGroupId());
    }

    private byte[] extractLogData(TransactionContext trx) {
        if (trx.getLogEntry() == null || !trx.getLogEntry().hasStateMachineLogEntry()) {
            return new byte[0];
        }
        return trx.getLogEntry().getStateMachineLogEntry().getLogData().toByteArray();
    }

    private CompletionStage<Void> handleCommand(RaftCommandEnvelope envelope) {
        return switch (envelope.type()) {
            case LOCK_ACQUIRE -> {
                LockCommand cmd = (LockCommand) envelope.command();
                yield lockService.tryAcquire(cmd.name(), cmd.ownerNodeId(), Duration.ofMillis(cmd.leaseMillis()), cmd.metadata())
                        .then()
                        .toFuture();
            }
            case LOCK_RELEASE -> {
                ReleaseLockCommand cmd = (ReleaseLockCommand) envelope.command();
                yield lockService.release(cmd.name(), cmd.ownerNodeId()).then().toFuture();
            }
            case ASSIGN_PARTITION -> {
                AssignPartitionCommand cmd = (AssignPartitionCommand) envelope.command();
                Assignment assignment = new Assignment(cmd.groupId(), cmd.partition(), cmd.ownerNodeId(),
                        cmd.equipmentIds(), cmd.workflowHandoff(), 0L, clock.instant());
                yield assignmentService.applyAssignments(cmd.groupId(), java.util.List.of(assignment)).toFuture();
            }
            case REVOKE_PARTITION -> {
                RevokePartitionCommand cmd = (RevokePartitionCommand) envelope.command();
                yield assignmentService.revokeAssignments(cmd.groupId(), java.util.List.of(cmd.partition()), cmd.reason())
                        .toFuture();
            }
            case ASSIGN_KEY -> {
                AssignKeyCommand cmd = (AssignKeyCommand) envelope.command();
                yield assignmentService.assignKey(cmd.groupId(), cmd.partition(), cmd.key(), cmd.appId(), cmd.source())
                        .then()
                        .toFuture();
            }
            case UNASSIGN_KEY -> {
                UnassignKeyCommand cmd = (UnassignKeyCommand) envelope.command();
                yield assignmentService.unassignKey(cmd.groupId(), cmd.partition(), cmd.key())
                        .then()
                        .toFuture();
            }
        };
    }

    private void maybeTriggerSnapshot() {
        if (snapshotThreshold <= 0) {
            return;
        }
        long applied = appliedSinceSnapshot.incrementAndGet();
        if (applied < snapshotThreshold) {
            return;
        }
        try {
            takeSnapshot();
        } catch (IOException e) {
            log.warn("Auto snapshot failed for group {}", groupIdAsString(), e);
        }
    }

    private void loadSnapshot() {
        try {
            snapshotStore.loadLatest(groupIdAsString())
                    .ifPresent(snapshot -> {
                        if (snapshot.formatVersion() != SNAPSHOT_VERSION) {
                            log.warn("Snapshot version mismatch for group {} (expected {}, found {})",
                                    groupIdAsString(), SNAPSHOT_VERSION, snapshot.formatVersion());
                            return;
                        }
                        restoreSnapshot(snapshot);
                    });
        } catch (IOException e) {
            log.warn("Failed to load snapshot for group {}", groupIdAsString(), e);
        }
    }

    private void restoreSnapshot(ClusterSnapshot snapshot) {
        assignmentService.restoreSnapshot(snapshot.groupId(), snapshot.assignments(), snapshot.keyAssignments());
        lockService.restoreLocks(snapshot.locks());
        metrics.setGroupId(snapshot.groupId());
        metrics.recordApply(snapshot.index(), snapshot.takenAt());
        metrics.recordSnapshot(snapshot.index(), snapshot.takenAt());
        metrics.setSafeMode(safeModeGuard.isSafeMode(), safeModeGuard.reason());
        if (snapshot.term() >= 0 && snapshot.index() >= 0) {
            updateLastAppliedTermIndex(snapshot.term(), snapshot.index());
        }
        appliedSinceSnapshot.set(0L);
        log.info("Restored snapshot for group {} at term={}, index={}, takenAt={}", snapshot.groupId(),
                snapshot.term(), snapshot.index(), snapshot.takenAt());
    }

    @Override
    public void close() throws IOException {
        metrics.markStopped(clock.instant());
        super.close();
    }

    public void enterSafeMode(String reason) {
        safeModeGuard.enterSafeMode(reason);
        metrics.setSafeMode(true, reason);
    }

    public void exitSafeMode() {
        safeModeGuard.exitSafeMode();
        metrics.setSafeMode(false, "");
    }

    public LeaderProcessingGuard leaderProcessingGuard(LeaderElectionService leaderElectionService, String nodeId) {
        return new LeaderProcessingGuard(leaderElectionService, groupIdAsString(), nodeId, safeModeGuard);
    }

    private String groupIdAsString() {
        RaftGroupId groupId = getGroupId();
        if (groupId == null) {
            return "unknown";
        }
        return groupId.getUuid().toString();
    }
}
