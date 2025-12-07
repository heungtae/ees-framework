package com.ees.cluster.assignment;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.TopologyEvent;
import com.ees.cluster.model.TopologyEventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAssignmentService implements AssignmentService {

    private final Map<String, Map<Integer, Assignment>> assignments = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Map<String, KeyAssignment>>> keyAssignments = new ConcurrentHashMap<>();
    private final Sinks.Many<TopologyEvent> eventSink =
            Sinks.many().multicast().onBackpressureBuffer();
    private final Clock clock;

    public InMemoryAssignmentService() {
        this(Clock.systemUTC());
    }

    public InMemoryAssignmentService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Mono<Void> applyAssignments(String groupId, Collection<Assignment> updates) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(updates, "updates must not be null");
        Instant now = clock.instant();
        Map<Integer, Assignment> groupAssignments = assignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        return Flux.fromIterable(updates)
                .doOnNext(update -> {
                    Assignment current = groupAssignments.get(update.partition());
                    long version = current == null ? 1L : current.version() + 1;
                    Assignment newAssignment = new Assignment(groupId, update.partition(), update.ownerNodeId(),
                            update.equipmentIds(), update.workflowHandoff(), version, now);
                    groupAssignments.put(update.partition(), newAssignment);
                    TopologyEventType type = current == null ? TopologyEventType.ASSIGNED : TopologyEventType.UPDATED;
                    emitEvent(new TopologyEvent(type, newAssignment, null, now));
                })
                .then();
    }

    @Override
    public Mono<Void> revokeAssignments(String groupId, Collection<Integer> partitions, String reason) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        Map<Integer, Assignment> groupAssignments = assignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        return Flux.fromIterable(partitions)
                .doOnNext(partition -> {
                    Assignment removed = groupAssignments.remove(partition);
                    Map<Integer, Map<String, KeyAssignment>> groupKeyAssignments =
                            keyAssignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
                    groupKeyAssignments.remove(partition);
                    if (removed != null) {
                        emitEvent(new TopologyEvent(TopologyEventType.REVOKED, removed, null, clock.instant()));
                    }
                })
                .then();
    }

    @Override
    public Mono<Optional<Assignment>> findAssignment(String groupId, int partition) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Map<Integer, Assignment> groupAssignments = assignments.getOrDefault(groupId, Map.of());
        return Mono.just(Optional.ofNullable(groupAssignments.get(partition)));
    }

    @Override
    public Mono<KeyAssignment> assignKey(String groupId, int partition, String key, String appId, KeyAssignmentSource source) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Instant now = clock.instant();
        Map<Integer, Map<String, KeyAssignment>> group = keyAssignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> partitionAssignments = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        KeyAssignment current = partitionAssignments.get(key);
        long version = current == null ? 1L : current.version() + 1;
        KeyAssignment updated = new KeyAssignment(groupId, partition, key, appId, source, version, now);
        partitionAssignments.put(key, updated);
        Assignment assignment = assignments.getOrDefault(groupId, Map.of()).get(partition);
        emitEvent(new TopologyEvent(TopologyEventType.KEY_ASSIGNED, assignment, updated, now));
        return Mono.just(updated);
    }

    @Override
    public Mono<Optional<KeyAssignment>> getKeyAssignment(String groupId, int partition, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, KeyAssignment>> group = keyAssignments.getOrDefault(groupId, Map.of());
        Map<String, KeyAssignment> partitionAssignments = group.getOrDefault(partition, Map.of());
        return Mono.just(Optional.ofNullable(partitionAssignments.get(key)));
    }

    @Override
    public Mono<Boolean> unassignKey(String groupId, int partition, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, KeyAssignment>> group = keyAssignments.get(groupId);
        if (group == null) {
            return Mono.just(false);
        }
        Map<String, KeyAssignment> partitionAssignments = group.get(partition);
        if (partitionAssignments == null) {
            return Mono.just(false);
        }
        KeyAssignment removed = partitionAssignments.remove(key);
        if (removed != null) {
            Assignment assignment = assignments.getOrDefault(groupId, Map.of()).get(partition);
            emitEvent(new TopologyEvent(TopologyEventType.KEY_UNASSIGNED, assignment, removed, clock.instant()));
        }
        return Mono.just(removed != null);
    }

    @Override
    public Flux<TopologyEvent> topologyEvents() {
        return eventSink.asFlux();
    }

    private void emitEvent(TopologyEvent event) {
        eventSink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
