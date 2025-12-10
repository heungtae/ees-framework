package com.ees.cluster.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

public record KeyAssignment(
        String groupId,
        int partition,
        String kind,
        String key,
        String appId,
        KeyAssignmentSource assignedBy,
        long version,
        Instant updatedAt
) {

    public KeyAssignment {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(assignedBy, "assignedBy must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    @JsonCreator
    public static KeyAssignment create(@JsonProperty("groupId") String groupId,
                                       @JsonProperty("partition") int partition,
                                       @JsonProperty("kind") String kind,
                                       @JsonProperty("key") String key,
                                       @JsonProperty("appId") String appId,
                                       @JsonProperty("assignedBy") KeyAssignmentSource assignedBy,
                                       @JsonProperty("version") long version,
                                       @JsonProperty("updatedAt") Instant updatedAt) {
        String resolvedKind = kind != null ? kind : DEFAULT;
        return new KeyAssignment(groupId, partition, resolvedKind, key, appId, assignedBy, version, updatedAt);
    }
}
