package com.ees.cluster.raft;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderInfo;

import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enforces leader-only processing for a Raft group with an optional safe mode guard.
 */
public class LeaderProcessingGuard {

    private final LeaderElectionService leaderElectionService;
    private final String groupId;
    private final String nodeId;
    private final RebalanceSafeModeGuard safeModeGuard;
    private final RaftStateMachineMetrics metrics;
    // AtomicBoolean 동작을 수행한다.
    private final AtomicBoolean lastAllowed = new AtomicBoolean(false);
    /**
     * 인스턴스를 생성한다.
     * @param leaderElectionService 
     * @param groupId 
     * @param nodeId 
     * @param safeModeGuard 
     * @param metrics 
     */

    public LeaderProcessingGuard(LeaderElectionService leaderElectionService,
                                 String groupId,
                                 String nodeId,
                                 RebalanceSafeModeGuard safeModeGuard,
                                 RaftStateMachineMetrics metrics) {
        this.leaderElectionService = Objects.requireNonNull(leaderElectionService, "leaderElectionService must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.safeModeGuard = Objects.requireNonNull(safeModeGuard, "safeModeGuard must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    }
    /**
     * allowProcessing를 수행한다.
     * @return 
     */

    public ProcessingDecision allowProcessing() {
        if (safeModeGuard.isSafeMode()) {
            return ProcessingDecision.denied("safe-mode:" + safeModeGuard.reason());
        }
        Optional<LeaderInfo> optional = leaderElectionService.getLeader(groupId);
        if (optional.filter(info -> isSelfLeader(info, nodeId)).isPresent()) {
            recordLeader(optional.get().leaderNodeId(), true);
            return ProcessingDecision.allowed("leader");
        }
        recordLeader(optional.map(LeaderInfo::leaderNodeId).orElse(""), false);
        return ProcessingDecision.denied("not-leader");
    }
    // selfLeader 여부를 반환한다.

    private boolean isSelfLeader(LeaderInfo info, String nodeId) {
        return info.leaderNodeId().equals(nodeId);
    }
    // recordLeader 동작을 수행한다.

    private void recordLeader(String leaderId, boolean allowed) {
        metrics.updateLeader(leaderId);
        boolean previous = lastAllowed.getAndSet(allowed);
        if (previous != allowed) {
            org.slf4j.LoggerFactory.getLogger(LeaderProcessingGuard.class)
                    .info("Leader processing state changed for group {}: allowed={} leader={}", groupId, allowed, leaderId);
        }
    }
}
