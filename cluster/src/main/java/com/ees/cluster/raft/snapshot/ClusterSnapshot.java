package com.ees.cluster.raft.snapshot;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.LockRecord;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

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
        Map<Integer, Map<String, KeyAssignment>> keyAssignments
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
}
