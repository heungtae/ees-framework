package com.ees.cluster.kafka;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.WorkflowHandoff;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

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

    public Mono<Void> onPartitionsAssigned(String groupId, String nodeId, Collection<Integer> partitions) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        Instant now = clock.instant();
        return assignmentService.applyAssignments(groupId,
                partitions.stream()
                        .map(partition -> new Assignment(groupId, partition, nodeId, Collections.emptyList(),
                                (WorkflowHandoff) null, 0L, now))
                        .toList());
    }

    public Mono<Void> onPartitionsRevoked(String groupId, Collection<Integer> partitions, String reason) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        return assignmentService.revokeAssignments(groupId, partitions, reason);
    }
}
