package com.ees.cluster.kafka;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.model.WorkflowHandoff;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Bridge Kafka consumer rebalance events to AssignmentService updates.
 */
public class KafkaConsumerAssignmentListener implements ConsumerRebalanceListener {

    private final AssignmentService assignmentService;
    private final String groupId;
    private final String nodeId;
    private final Clock clock;
    private final Map<String, java.util.List<String>> defaultAffinities;

    public KafkaConsumerAssignmentListener(AssignmentService assignmentService, String groupId, String nodeId) {
        this(assignmentService, groupId, nodeId, Clock.systemUTC());
    }

    public KafkaConsumerAssignmentListener(AssignmentService assignmentService, String groupId, String nodeId, Clock clock) {
        this(assignmentService, groupId, nodeId, clock, Map.of());
    }

    public KafkaConsumerAssignmentListener(AssignmentService assignmentService, String groupId, String nodeId,
                                           Clock clock, Map<String, java.util.List<String>> defaultAffinities) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.defaultAffinities = Collections.unmodifiableMap(new java.util.HashMap<>(
                Objects.requireNonNull(defaultAffinities, "defaultAffinities must not be null")));
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        assignmentService.revokeAssignments(groupId,
            partitions.stream().map(TopicPartition::partition).collect(Collectors.toList()),
            "kafka-rebalance");
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        Instant now = clock.instant();
        assignmentService.applyAssignments(groupId,
            partitions.stream()
                .map(tp -> new com.ees.cluster.model.Assignment(
                    groupId,
                    tp.partition(),
                    nodeId,
                    defaultAffinities,
                    (WorkflowHandoff) null,
                    0L,
                    now))
                .toList());
    }
}
