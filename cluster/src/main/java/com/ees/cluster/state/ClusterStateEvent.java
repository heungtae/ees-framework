package com.ees.cluster.state;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ClusterStateEvent(
        String key,
        ClusterStateEventType type,
        Optional<Object> value,
        Instant emittedAt
) {

    public ClusterStateEvent {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(emittedAt, "emittedAt must not be null");
    }
}
