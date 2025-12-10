package com.ees.cluster.kafka;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.WorkflowHandoff;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Map;

/**
 * Lightweight helper to translate Kafka consumer callbacks into AssignmentService updates.
 * This does not depend on Kafka classes to keep the module light; real apps should wire
 * their consumer listener to these entrypoints.
 */
public class KafkaAssignmentCoordinator {

    private final AssignmentService assignmentService;
    private final Clock clock;

    public KafkaAssignmentCoordinator(AssignmentService assignmentService) {
        this(assignmentService, Clock.systemUTC());
    }

    public KafkaAssignmentCoordinator(AssignmentService assignmentService, Clock clock) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void onPartitionsAssigned(String groupId, String nodeId, Collection<Integer> partitions) {
        onPartitionsAssigned(groupId, nodeId, partitions, Map.of());
    }

    public void onPartitionsAssigned(String groupId, String nodeId, Collection<Integer> partitions,
                                     Map<String, java.util.List<String>> affinities) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        Objects.requireNonNull(affinities, "affinities must not be null");
        Instant now = clock.instant();
        assignmentService.applyAssignments(groupId,
                partitions.stream()
                        .map(partition -> new Assignment(groupId, partition, nodeId, affinities.isEmpty() ? Collections.emptyMap() : affinities,
                                (WorkflowHandoff) null, 0L, now))
                        .toList());
    }

    public void onPartitionsRevoked(String groupId, Collection<Integer> partitions, String reason) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        assignmentService.revokeAssignments(groupId, partitions, reason);
    }
}
