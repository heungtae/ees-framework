package com.ees.cluster.raft;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderInfo;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Enforces leader-only processing for a Raft group with an optional safe mode guard.
 */
public class LeaderProcessingGuard {

    private final LeaderElectionService leaderElectionService;
    private final String groupId;
    private final String nodeId;
    private final RebalanceSafeModeGuard safeModeGuard;

    public LeaderProcessingGuard(LeaderElectionService leaderElectionService,
                                 String groupId,
                                 String nodeId,
                                 RebalanceSafeModeGuard safeModeGuard) {
        this.leaderElectionService = Objects.requireNonNull(leaderElectionService, "leaderElectionService must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.safeModeGuard = Objects.requireNonNull(safeModeGuard, "safeModeGuard must not be null");
    }

    public Mono<ProcessingDecision> allowProcessing() {
        if (safeModeGuard.isSafeMode()) {
            return Mono.just(ProcessingDecision.denied("safe-mode:" + safeModeGuard.reason()));
        }
        return leaderElectionService.getLeader(groupId)
                .map(optional -> optional
                        .filter(info -> isSelfLeader(info, nodeId))
                        .map(info -> ProcessingDecision.allowed("leader"))
                        .orElseGet(() -> ProcessingDecision.denied("not-leader")));
    }

    private boolean isSelfLeader(LeaderInfo info, String nodeId) {
        return info.leaderNodeId().equals(nodeId);
    }
}
