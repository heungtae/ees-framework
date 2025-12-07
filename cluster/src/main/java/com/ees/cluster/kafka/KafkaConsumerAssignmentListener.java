package com.ees.cluster.kafka;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.model.WorkflowHandoff;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
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

    public KafkaConsumerAssignmentListener(AssignmentService assignmentService, String groupId, String nodeId) {
        this(assignmentService, groupId, nodeId, Clock.systemUTC());
    }

    public KafkaConsumerAssignmentListener(AssignmentService assignmentService, String groupId, String nodeId, Clock clock) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        Mono<Void> revoke = assignmentService.revokeAssignments(groupId,
                partitions.stream().map(TopicPartition::partition).collect(Collectors.toList()),
                "kafka-rebalance");
        revoke.block();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        Instant now = clock.instant();
        Mono<Void> apply = assignmentService.applyAssignments(groupId,
                partitions.stream()
                        .map(tp -> new com.ees.cluster.model.Assignment(
                                groupId,
                                tp.partition(),
                                nodeId,
                                Collections.emptyList(),
                                (WorkflowHandoff) null,
                                0L,
                                now))
                        .toList());
        apply.block();
    }
}
