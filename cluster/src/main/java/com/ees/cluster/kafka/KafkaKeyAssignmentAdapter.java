package com.ees.cluster.kafka;

import com.ees.cluster.assignment.AffinityKeyExtractor;
import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;

import java.util.Objects;
import java.util.Optional;

/**
 * Helper to assign keys for Kafka records using a provided affinity extractor.
 * Keeps the module Kafka-agnostic by leaving record type generic.
 */
public class KafkaKeyAssignmentAdapter<T> {

    private final AssignmentService assignmentService;
    private final AffinityKeyExtractor<T> extractor;
    private final String groupId;
    /**
     * KafkaKeyAssignmentAdapter를 수행한다.
     * @param assignmentService 
     * @param groupId 
     * @param extractor 
     * @return 
     */

    public KafkaKeyAssignmentAdapter(AssignmentService assignmentService,
                                     String groupId,
                                     AffinityKeyExtractor<T> extractor) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.extractor = Objects.requireNonNull(extractor, "extractor must not be null");
    }
    /**
     * assignKey를 수행한다.
     * @param partition 
     * @param record 
     * @param appId 
     * @param source 
     * @return 
     */

    public Optional<KeyAssignment> assignKey(int partition, T record, String appId, KeyAssignmentSource source) {
        Objects.requireNonNull(source, "source must not be null");
        String key = extractor.extract(record);
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assignmentService.assignKey(groupId, partition, extractor.kind(), key, appId, source));
    }
}
