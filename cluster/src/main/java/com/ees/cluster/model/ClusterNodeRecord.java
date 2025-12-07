package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

public record ClusterNodeRecord(
        ClusterNode node,
        ClusterNodeStatus status,
        Instant joinedAt,
        Instant lastHeartbeat
) {

    public ClusterNodeRecord {
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        Objects.requireNonNull(lastHeartbeat, "lastHeartbeat must not be null");
    }

    public ClusterNodeRecord withStatus(ClusterNodeStatus newStatus) {
        return new ClusterNodeRecord(node, newStatus, joinedAt, lastHeartbeat);
    }

    public ClusterNodeRecord withLastHeartbeat(Instant heartbeat) {
        return new ClusterNodeRecord(node, status, joinedAt, heartbeat);
    }
}
