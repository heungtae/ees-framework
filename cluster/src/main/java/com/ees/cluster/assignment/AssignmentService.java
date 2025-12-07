package com.ees.cluster.assignment;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.TopologyEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Optional;

public interface AssignmentService {

    Mono<Void> applyAssignments(String groupId, Collection<Assignment> assignments);

    Mono<Void> revokeAssignments(String groupId, Collection<Integer> partitions, String reason);

    Mono<Optional<Assignment>> findAssignment(String groupId, int partition);

    Mono<KeyAssignment> assignKey(String groupId, int partition, String key, String appId, KeyAssignmentSource source);

    Mono<Optional<KeyAssignment>> getKeyAssignment(String groupId, int partition, String key);

    Mono<Boolean> unassignKey(String groupId, int partition, String key);

    Flux<TopologyEvent> topologyEvents();
}
