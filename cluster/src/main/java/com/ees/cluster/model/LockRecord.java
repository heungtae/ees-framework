package com.ees.cluster.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record LockRecord(
        String name,
        String ownerNodeId,
        Instant leaseUntil,
        Map<String, String> metadata
) {

    public LockRecord {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        Objects.requireNonNull(leaseUntil, "leaseUntil must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        metadata = Collections.unmodifiableMap(Map.copyOf(metadata));
    }

    public boolean isExpired(Instant now) {
        return leaseUntil.isBefore(now) || leaseUntil.equals(now);
    }
}
