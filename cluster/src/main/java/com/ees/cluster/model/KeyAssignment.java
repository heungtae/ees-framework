package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

public record KeyAssignment(
        String groupId,
        int partition,
        String key,
        String appId,
        KeyAssignmentSource assignedBy,
        long version,
        Instant updatedAt
) {

    public KeyAssignment {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(assignedBy, "assignedBy must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
