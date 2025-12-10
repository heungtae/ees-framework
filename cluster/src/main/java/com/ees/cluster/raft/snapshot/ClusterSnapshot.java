package com.ees.cluster.raft.snapshot;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.LockRecord;
import com.ees.cluster.model.KeyAssignmentSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

/**
 * Snapshot payload persisted by the Raft state machine. The format is versioned to allow upgrades.
 */
public record ClusterSnapshot(
        long formatVersion,
        String groupId,
        long term,
        long index,
        Instant takenAt,
        Map<String, LockRecord> locks,
        Map<Integer, Assignment> assignments,
        Map<Integer, Map<String, Map<String, KeyAssignment>>> keyAssignments
) {

    public ClusterSnapshot {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(takenAt, "takenAt must not be null");
        Objects.requireNonNull(locks, "locks must not be null");
        Objects.requireNonNull(assignments, "assignments must not be null");
        Objects.requireNonNull(keyAssignments, "keyAssignments must not be null");
        locks = Map.copyOf(locks);
        assignments = Map.copyOf(assignments);
        keyAssignments = Map.copyOf(keyAssignments);
    }

    @JsonCreator
    public static ClusterSnapshot create(@JsonProperty("formatVersion") long formatVersion,
                                         @JsonProperty("groupId") String groupId,
                                         @JsonProperty("term") long term,
                                         @JsonProperty("index") long index,
                                         @JsonProperty("takenAt") Instant takenAt,
                                         @JsonProperty("locks") Map<String, LockRecord> locks,
                                         @JsonProperty("assignments") Map<Integer, Assignment> assignments,
                                         @JsonProperty("keyAssignments") Map<Integer, ?> keyAssignments) {
        Map<Integer, Map<String, Map<String, KeyAssignment>>> normalized = normalizeKeyAssignments(keyAssignments);
        return new ClusterSnapshot(formatVersion, groupId, term, index, takenAt, locks, assignments, normalized);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Map<String, Map<String, KeyAssignment>>> normalizeKeyAssignments(Map<Integer, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Map<String, Map<String, KeyAssignment>>> normalized = new HashMap<>();
        raw.forEach((partition, value) -> {
            if (!(value instanceof Map<?, ?> map)) {
                return;
            }
            Map<String, Map<String, KeyAssignment>> byKind = new HashMap<>();
            boolean legacyShape = map.values().stream().noneMatch(v -> v instanceof Map);
            if (legacyShape) {
                Map<String, KeyAssignment> defaultKind = new HashMap<>();
                map.forEach((key, assignment) -> {
                    KeyAssignment converted = convertAssignment(assignment, key, partition);
                    if (converted != null) {
                        defaultKind.put(String.valueOf(key), converted);
                    }
                });
                if (!defaultKind.isEmpty()) {
                    byKind.put(DEFAULT, defaultKind);
                }
            } else {
                map.forEach((kind, keyedAssignments) -> {
                    if (kind == null || !(keyedAssignments instanceof Map<?, ?> typedMap)) {
                        return;
                    }
                    Map<String, KeyAssignment> converted = new HashMap<>();
                    typedMap.forEach((key, assignment) -> {
                        KeyAssignment typed = convertAssignment(assignment, key, partition);
                        if (typed != null) {
                            converted.put(String.valueOf(key), typed);
                        }
                    });
                    byKind.put(String.valueOf(kind), converted);
                });
            }
            if (!byKind.isEmpty()) {
                normalized.put(partition, byKind);
            }
        });
        return normalized;
    }

    private static KeyAssignment convertAssignment(Object assignment, Object key, Integer partition) {
        if (assignment instanceof KeyAssignment typed) {
            return typed;
        }
        if (assignment instanceof Map<?, ?> map) {
            String groupId = map.get("groupId") != null ? map.get("groupId").toString() : null;
            String kind = map.get("kind") != null ? map.get("kind").toString() : DEFAULT;
            String keyValue = key != null ? key.toString() : (map.get("key") != null ? map.get("key").toString() : null);
            String appId = map.get("appId") != null ? map.get("appId").toString() : null;
            Object versionRaw = map.get("version");
            long version = versionRaw instanceof Number number ? number.longValue() : 1L;
            Object sourceRaw = map.get("assignedBy");
            KeyAssignmentSource source = sourceRaw != null
                    ? KeyAssignmentSource.valueOf(sourceRaw.toString())
                    : KeyAssignmentSource.AUTO;
            Object updatedAtRaw = map.get("updatedAt");
            Instant updatedAt;
            if (updatedAtRaw instanceof Instant instant) {
                updatedAt = instant;
            } else if (updatedAtRaw instanceof Number number) {
                long epoch = number.longValue();
                updatedAt = epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
            } else if (updatedAtRaw != null) {
                updatedAt = Instant.parse(updatedAtRaw.toString());
            } else {
                updatedAt = null;
            }
            int partitionId = partition != null ? partition : (map.get("partition") instanceof Number number ? number.intValue() : 0);
            if (groupId == null || keyValue == null || appId == null || updatedAt == null) {
                return null;
            }
            return new KeyAssignment(groupId, partitionId, kind, keyValue, appId, source, version, updatedAt);
        }
        return null;
    }
}
