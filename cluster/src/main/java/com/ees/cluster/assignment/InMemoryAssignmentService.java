package com.ees.cluster.assignment;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.TopologyEvent;
import com.ees.cluster.model.TopologyEventType;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class InMemoryAssignmentService implements AssignmentService {

    private final Map<String, Map<Integer, Assignment>> assignments = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Map<String, Map<String, KeyAssignment>>>> keyAssignments = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<TopologyEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public InMemoryAssignmentService() {
        this(Clock.systemUTC());
    }

    public InMemoryAssignmentService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void applyAssignments(String groupId, Collection<Assignment> updates) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(updates, "updates must not be null");
        Instant now = clock.instant();
        Map<Integer, Assignment> groupAssignments = assignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        for (Assignment update : updates) {
            Assignment current = groupAssignments.get(update.partition());
            long version = current == null ? 1L : current.version() + 1;
            Assignment newAssignment = new Assignment(groupId, update.partition(), update.ownerNodeId(),
                update.affinities(), update.workflowHandoff(), version, now);
            groupAssignments.put(update.partition(), newAssignment);
            TopologyEventType type = current == null ? TopologyEventType.ASSIGNED : TopologyEventType.UPDATED;
            emitEvent(new TopologyEvent(type, newAssignment, null, now));
        }
    }

    @Override
    public void revokeAssignments(String groupId, Collection<Integer> partitions, String reason) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        Map<Integer, Assignment> groupAssignments = assignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        for (Integer partition : partitions) {
            Assignment removed = groupAssignments.remove(partition);
            Map<Integer, Map<String, Map<String, KeyAssignment>>> groupKeyAssignments =
                keyAssignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
            groupKeyAssignments.remove(partition);
            if (removed != null) {
                emitEvent(new TopologyEvent(TopologyEventType.REVOKED, removed, null, clock.instant()));
            }
        }
    }

    @Override
    public Optional<Assignment> findAssignment(String groupId, int partition) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Map<Integer, Assignment> groupAssignments = assignments.getOrDefault(groupId, Map.of());
        return Optional.ofNullable(groupAssignments.get(partition));
    }

    @Override
    public KeyAssignment assignKey(String groupId, int partition, String kind, String key, String appId, KeyAssignmentSource source) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Instant now = clock.instant();
        Map<Integer, Map<String, Map<String, KeyAssignment>>> group = keyAssignments.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, Map<String, KeyAssignment>> partitionAssignments = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> kindAssignments = partitionAssignments.computeIfAbsent(kind, ignored -> new ConcurrentHashMap<>());
        KeyAssignment current = kindAssignments.get(key);
        long version = current == null ? 1L : current.version() + 1;
        KeyAssignment updated = new KeyAssignment(groupId, partition, kind, key, appId, source, version, now);
        kindAssignments.put(key, updated);
        Assignment assignment = assignments.getOrDefault(groupId, Map.of()).get(partition);
        emitEvent(new TopologyEvent(TopologyEventType.KEY_ASSIGNED, assignment, updated, now));
        return updated;
    }

    @Override
    public Optional<KeyAssignment> getKeyAssignment(String groupId, int partition, String kind, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, Map<String, KeyAssignment>>> group = keyAssignments.getOrDefault(groupId, Map.of());
        Map<String, Map<String, KeyAssignment>> partitionAssignments = group.getOrDefault(partition, Map.of());
        Map<String, KeyAssignment> kindAssignments = partitionAssignments.getOrDefault(kind, Map.of());
        return Optional.ofNullable(kindAssignments.get(key));
    }

    @Override
    public boolean unassignKey(String groupId, int partition, String kind, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, Map<String, KeyAssignment>>> group = keyAssignments.get(groupId);
        if (group == null) {
            return false;
        }
        Map<String, Map<String, KeyAssignment>> partitionAssignments = group.get(partition);
        if (partitionAssignments == null) {
            return false;
        }
        Map<String, KeyAssignment> kindAssignments = partitionAssignments.get(kind);
        if (kindAssignments == null) {
            return false;
        }
        KeyAssignment removed = kindAssignments.remove(key);
        if (removed != null) {
            Assignment assignment = assignments.getOrDefault(groupId, Map.of()).get(partition);
            emitEvent(new TopologyEvent(TopologyEventType.KEY_UNASSIGNED, assignment, removed, clock.instant()));
        }
        return removed != null;
    }

    @Override
    public void topologyEvents(Consumer<TopologyEvent> consumer) {
        listeners.add(consumer);
    }

    private void emitEvent(TopologyEvent event) {
        for (Consumer<TopologyEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
