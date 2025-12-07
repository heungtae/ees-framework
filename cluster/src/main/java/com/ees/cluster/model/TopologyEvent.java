package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

public record TopologyEvent(
        TopologyEventType type,
        Assignment assignment,
        KeyAssignment keyAssignment,
        Instant emittedAt
) {

    public TopologyEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(emittedAt, "emittedAt must not be null");
    }
}
