package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

public record MembershipEvent(
        MembershipEventType type,
        ClusterNodeRecord node,
        Instant emittedAt
) {

    public MembershipEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(emittedAt, "emittedAt must not be null");
    }
}
