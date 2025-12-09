package com.ees.cluster.raft;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderInfo;
import reactor.core.publisher.Mono;

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
    private final AtomicBoolean lastAllowed = new AtomicBoolean(false);

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

    public Mono<ProcessingDecision> allowProcessing() {
        if (safeModeGuard.isSafeMode()) {
            return Mono.just(ProcessingDecision.denied("safe-mode:" + safeModeGuard.reason()));
        }
        return leaderElectionService.getLeader(groupId)
                .map(optional -> optional
                        .filter(info -> isSelfLeader(info, nodeId))
                        .map(info -> {
                            recordLeader(info.leaderNodeId(), true);
                            return ProcessingDecision.allowed("leader");
                        })
                        .orElseGet(() -> {
                            recordLeader(optional.map(LeaderInfo::leaderNodeId).orElse(""), false);
                            return ProcessingDecision.denied("not-leader");
                        }));
    }

    private boolean isSelfLeader(LeaderInfo info, String nodeId) {
        return info.leaderNodeId().equals(nodeId);
    }

    private void recordLeader(String leaderId, boolean allowed) {
        metrics.updateLeader(leaderId);
        boolean previous = lastAllowed.getAndSet(allowed);
        if (previous != allowed) {
            org.slf4j.LoggerFactory.getLogger(LeaderProcessingGuard.class)
                    .info("Leader processing state changed for group {}: allowed={} leader={}", groupId, allowed, leaderId);
        }
    }
}
