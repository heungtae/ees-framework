package com.ees.cluster.raft;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.TopologyEvent;
import com.ees.cluster.model.TopologyEventType;
import com.ees.cluster.state.ClusterStateRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Repository-backed assignment tracker intended to mirror Raft state machine entries.
 * This keeps a local cache for fast lookups while persisting snapshots into the repository.
 */
public class RaftAssignmentService implements AssignmentService {

    private static final String ASSIGNMENTS_PREFIX = "cluster:assignments/";
    private static final String KEY_ASSIGNMENTS_PREFIX = "cluster:key-assignments/";

    private final ClusterStateRepository repository;
    private final Clock clock;
    private final CopyOnWriteArrayList<Consumer<TopologyEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Map<Integer, Assignment>> cache = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Map<String, Map<String, KeyAssignment>>>> keyCache = new ConcurrentHashMap<>();
    private final Duration ttl;

    public RaftAssignmentService(ClusterStateRepository repository, Duration ttl) {
        this(repository, ttl, Clock.systemUTC());
    }

    public RaftAssignmentService(ClusterStateRepository repository, Duration ttl, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.ttl = Objects.requireNonNull(ttl, "ttl must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void applyAssignments(String groupId, Collection<Assignment> assignments) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(assignments, "assignments must not be null");
        Instant now = clock.instant();
        Map<Integer, Assignment> group = cache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        assignments.forEach(assignment -> {
            Assignment current = group.get(assignment.partition());
            long version = current == null ? 1L : current.version() + 1;
            Assignment updated = new Assignment(groupId, assignment.partition(), assignment.ownerNodeId(),
                assignment.affinities(), assignment.workflowHandoff(), version, now);
            group.put(assignment.partition(), updated);
            String key = assignmentKey(groupId, assignment.partition());
            repository.put(key, updated, ttl);
            emit(new TopologyEvent(
                current == null ? TopologyEventType.ASSIGNED : TopologyEventType.UPDATED,
                updated,
                null,
                now));
        });
    }

    @Override
    public void revokeAssignments(String groupId, Collection<Integer> partitions, String reason) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        Map<Integer, Assignment> group = cache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        partitions.forEach(partition -> {
            Assignment removed = group.remove(partition);
            keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>()).remove(partition);
            boolean deleted = repository.delete(assignmentKey(groupId, partition));
            if (deleted && removed != null) {
                emit(new TopologyEvent(TopologyEventType.REVOKED, removed, null, clock.instant()));
            }
        });
    }

    @Override
    public Optional<Assignment> findAssignment(String groupId, int partition) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Map<Integer, Assignment> group = cache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Assignment cached = group.get(partition);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Assignment> loaded = repository.get(assignmentKey(groupId, partition), Assignment.class);
        loaded.ifPresent(value -> group.put(partition, value));
        return loaded;
    }

    @Override
    public KeyAssignment assignKey(String groupId, int partition, String kind, String key, String appId, KeyAssignmentSource source) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Instant now = clock.instant();
        Map<Integer, Map<String, Map<String, KeyAssignment>>> group = keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, Map<String, KeyAssignment>> partitionCache = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> kindCache = partitionCache.computeIfAbsent(kind, ignored -> new ConcurrentHashMap<>());
        KeyAssignment current = kindCache.get(key);
        long version = current == null ? 1L : current.version() + 1;
        KeyAssignment updated = new KeyAssignment(groupId, partition, kind, key, appId, source, version, now);
        kindCache.put(key, updated);
        repository.put(keyAssignmentKey(groupId, partition, kind, key), updated, ttl);
        emit(new TopologyEvent(TopologyEventType.KEY_ASSIGNED,
            cache.getOrDefault(groupId, Map.of()).get(partition),
            updated,
            now));
        return updated;
    }

    @Override
    public Optional<KeyAssignment> getKeyAssignment(String groupId, int partition, String kind, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, Map<String, KeyAssignment>>> group = keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, Map<String, KeyAssignment>> partitionCache = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> kindCache = partitionCache.computeIfAbsent(kind, ignored -> new ConcurrentHashMap<>());
        KeyAssignment cached = kindCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<KeyAssignment> loaded = repository.get(keyAssignmentKey(groupId, partition, kind, key), KeyAssignment.class);
        loaded.ifPresent(value -> kindCache.put(key, value));
        return loaded;
    }

    @Override
    public boolean unassignKey(String groupId, int partition, String kind, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, Map<String, KeyAssignment>>> group = keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, Map<String, KeyAssignment>> partitionCache = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> kindCache = partitionCache.computeIfAbsent(kind, ignored -> new ConcurrentHashMap<>());
        KeyAssignment removed = kindCache.remove(key);
        boolean deleted = repository.delete(keyAssignmentKey(groupId, partition, kind, key));
        if (deleted && removed != null) {
            emit(new TopologyEvent(TopologyEventType.KEY_UNASSIGNED,
                cache.getOrDefault(groupId, Map.of()).get(partition),
                removed,
                clock.instant()));
        }
        return deleted;
    }

    @Override
    public void topologyEvents(Consumer<TopologyEvent> consumer) {
        listeners.add(consumer);
    }

    /**
     * Snapshot view of current assignments for a group. Intended for state machine snapshots.
     */
    public Map<Integer, Assignment> snapshotAssignments(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Map<Integer, Assignment> group = cache.getOrDefault(groupId, Map.of());
        return Map.copyOf(group);
    }

    /**
     * Snapshot view of current key assignments for a group.
     */
    public Map<Integer, Map<String, Map<String, KeyAssignment>>> snapshotKeyAssignments(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Map<Integer, Map<String, Map<String, KeyAssignment>>> group = keyCache.getOrDefault(groupId, Map.of());
        return group.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .collect(Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        kindEntry -> Map.copyOf(kindEntry.getValue())
                                ))
                ));
    }

    /**
     * Restore assignments and key assignments from a snapshot into both cache and backing repository.
     */
    public void restoreSnapshot(String groupId,
                                Map<Integer, Assignment> assignments,
                                Map<Integer, Map<String, Map<String, KeyAssignment>>> keyAssignments) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(assignments, "assignments must not be null");
        Objects.requireNonNull(keyAssignments, "keyAssignments must not be null");
        Map<Integer, Assignment> group = cache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        group.clear();
        group.putAll(assignments);
        assignments.values().forEach(assignment ->
                repository.put(assignmentKey(groupId, assignment.partition()), assignment, ttl));

        Map<Integer, Map<String, Map<String, KeyAssignment>>> groupKeys =
                keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        groupKeys.clear();
        keyAssignments.forEach((partition, keys) -> {
            Map<String, Map<String, KeyAssignment>> copy = new ConcurrentHashMap<>();
            keys.forEach((kind, keyedValues) -> {
                Map<String, KeyAssignment> kindCopy = new ConcurrentHashMap<>(keyedValues);
                copy.put(kind, kindCopy);
                kindCopy.forEach((key, value) -> repository.put(keyAssignmentKey(groupId, partition, kind, key), value, ttl));
            });
            groupKeys.put(partition, copy);
        });
    }

    private String assignmentKey(String groupId, int partition) {
        return ASSIGNMENTS_PREFIX + groupId + "/" + partition;
    }

    private String keyAssignmentKey(String groupId, int partition, String kind, String key) {
        return KEY_ASSIGNMENTS_PREFIX + groupId + "/" + partition + "/" + kind + "/" + key;
    }

    private void emit(TopologyEvent event) {
        for (Consumer<TopologyEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
