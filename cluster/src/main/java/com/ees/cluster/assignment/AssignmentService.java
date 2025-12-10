package com.ees.cluster.assignment;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.TopologyEvent;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

public interface AssignmentService {

    String DEFAULT_AFFINITY_KIND = DEFAULT;

    void applyAssignments(String groupId, Collection<Assignment> assignments);

    void revokeAssignments(String groupId, Collection<Integer> partitions, String reason);

    Optional<Assignment> findAssignment(String groupId, int partition);

    default KeyAssignment assignKey(String groupId, int partition, String key, String appId, KeyAssignmentSource source) {
        return assignKey(groupId, partition, DEFAULT_AFFINITY_KIND, key, appId, source);
    }

    default <T> Optional<KeyAssignment> assignKey(String groupId,
                                                  int partition,
                                                  T record,
                                                  AffinityKeyExtractor<T> extractor,
                                                  String appId,
                                                  KeyAssignmentSource source) {
        Objects.requireNonNull(extractor, "extractor must not be null");
        String key = extractor.extract(record);
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assignKey(groupId, partition, extractor.kind(), key, appId, source));
    }

    KeyAssignment assignKey(String groupId, int partition, String kind, String key, String appId, KeyAssignmentSource source);

    default Optional<KeyAssignment> getKeyAssignment(String groupId, int partition, String key) {
        return getKeyAssignment(groupId, partition, DEFAULT_AFFINITY_KIND, key);
    }

    Optional<KeyAssignment> getKeyAssignment(String groupId, int partition, String kind, String key);

    default boolean unassignKey(String groupId, int partition, String key) {
        return unassignKey(groupId, partition, DEFAULT_AFFINITY_KIND, key);
    }

    boolean unassignKey(String groupId, int partition, String kind, String key);

    void topologyEvents(Consumer<TopologyEvent> consumer);
}
