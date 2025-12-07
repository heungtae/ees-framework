package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

public record LeaderInfo(
        String groupId,
        String leaderNodeId,
        LeaderElectionMode mode,
        long term,
        Instant electedAt,
        Instant leaseExpiresAt
) {

    public LeaderInfo {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(leaderNodeId, "leaderNodeId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(electedAt, "electedAt must not be null");
        Objects.requireNonNull(leaseExpiresAt, "leaseExpiresAt must not be null");
    }
}
