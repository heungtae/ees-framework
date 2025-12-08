package com.ees.cluster.raft;

import java.time.Instant;
import java.util.Objects;

public record RaftHealthSnapshot(
        String groupId,
        boolean running,
        String leaderId,
        long lastAppliedIndex,
        long lastSnapshotIndex,
        Instant lastAppliedAt,
        Instant lastSnapshotAt,
        boolean stale,
        boolean safeMode,
        String safeModeReason
) {

    public RaftHealthSnapshot {
        Objects.requireNonNull(groupId, "groupId must not be null");
    }
}
