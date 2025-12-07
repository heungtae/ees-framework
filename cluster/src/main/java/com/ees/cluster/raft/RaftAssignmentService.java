package com.ees.cluster.raft;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.TopologyEvent;
import com.ees.cluster.model.TopologyEventType;
import com.ees.cluster.state.ClusterStateRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository-backed assignment tracker intended to mirror Raft state machine entries.
 * This keeps a local cache for fast lookups while persisting snapshots into the repository.
 */
public class RaftAssignmentService implements AssignmentService {

    private static final String ASSIGNMENTS_PREFIX = "cluster:assignments/";
    private static final String KEY_ASSIGNMENTS_PREFIX = "cluster:key-assignments/";

    private final ClusterStateRepository repository;
    private final Clock clock;
    private final Sinks.Many<TopologyEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Map<String, Map<Integer, Assignment>> cache = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Map<String, KeyAssignment>>> keyCache = new ConcurrentHashMap<>();
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
    public Mono<Void> applyAssignments(String groupId, Collection<Assignment> assignments) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(assignments, "assignments must not be null");
        Instant now = clock.instant();
        Map<Integer, Assignment> group = cache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        return Flux.fromIterable(assignments)
                .flatMap(assignment -> {
                    Assignment current = group.get(assignment.partition());
                    long version = current == null ? 1L : current.version() + 1;
                    Assignment updated = new Assignment(groupId, assignment.partition(), assignment.ownerNodeId(),
                            assignment.equipmentIds(), assignment.workflowHandoff(), version, now);
                    group.put(assignment.partition(), updated);
                    String key = assignmentKey(groupId, assignment.partition());
                    return repository.put(key, updated, ttl)
                            .doOnSuccess(ignored -> emit(new TopologyEvent(
                                    current == null ? TopologyEventType.ASSIGNED : TopologyEventType.UPDATED,
                                    updated,
                                    null,
                                    now)));
                })
                .then();
    }

    @Override
    public Mono<Void> revokeAssignments(String groupId, Collection<Integer> partitions, String reason) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(partitions, "partitions must not be null");
        Map<Integer, Assignment> group = cache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        return Flux.fromIterable(partitions)
                .flatMap(partition -> {
                    Assignment removed = group.remove(partition);
                    keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>()).remove(partition);
                    return repository.delete(assignmentKey(groupId, partition))
                            .doOnSuccess(ignored -> {
                                if (removed != null) {
                                    emit(new TopologyEvent(TopologyEventType.REVOKED, removed, null, clock.instant()));
                                }
                            });
                })
                .then();
    }

    @Override
    public Mono<Optional<Assignment>> findAssignment(String groupId, int partition) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Map<Integer, Assignment> group = cache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Assignment cached = group.get(partition);
        if (cached != null) {
            return Mono.just(Optional.of(cached));
        }
        return repository.get(assignmentKey(groupId, partition), Assignment.class)
                .doOnNext(opt -> opt.ifPresent(value -> group.put(partition, value)));
    }

    @Override
    public Mono<KeyAssignment> assignKey(String groupId, int partition, String key, String appId, KeyAssignmentSource source) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Instant now = clock.instant();
        Map<Integer, Map<String, KeyAssignment>> group = keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> partitionCache = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        KeyAssignment current = partitionCache.get(key);
        long version = current == null ? 1L : current.version() + 1;
        KeyAssignment updated = new KeyAssignment(groupId, partition, key, appId, source, version, now);
        partitionCache.put(key, updated);
        return repository.put(keyAssignmentKey(groupId, partition, key), updated, ttl)
                .doOnSuccess(ignored -> emit(new TopologyEvent(TopologyEventType.KEY_ASSIGNED,
                        cache.getOrDefault(groupId, Map.of()).get(partition),
                        updated,
                        now)))
                .thenReturn(updated);
    }

    @Override
    public Mono<Optional<KeyAssignment>> getKeyAssignment(String groupId, int partition, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, KeyAssignment>> group = keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> partitionCache = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        KeyAssignment cached = partitionCache.get(key);
        if (cached != null) {
            return Mono.just(Optional.of(cached));
        }
        return repository.get(keyAssignmentKey(groupId, partition, key), KeyAssignment.class)
                .doOnNext(opt -> opt.ifPresent(value -> partitionCache.put(key, value)));
    }

    @Override
    public Mono<Boolean> unassignKey(String groupId, int partition, String key) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Map<Integer, Map<String, KeyAssignment>> group = keyCache.computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>());
        Map<String, KeyAssignment> partitionCache = group.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>());
        KeyAssignment removed = partitionCache.remove(key);
        return repository.delete(keyAssignmentKey(groupId, partition, key))
                .doOnSuccess(ignored -> {
                    if (removed != null) {
                        emit(new TopologyEvent(TopologyEventType.KEY_UNASSIGNED,
                                cache.getOrDefault(groupId, Map.of()).get(partition),
                                removed,
                                clock.instant()));
                    }
                });
    }

    @Override
    public Flux<TopologyEvent> topologyEvents() {
        return sink.asFlux();
    }

    private String assignmentKey(String groupId, int partition) {
        return ASSIGNMENTS_PREFIX + groupId + "/" + partition;
    }

    private String keyAssignmentKey(String groupId, int partition, String key) {
        return KEY_ASSIGNMENTS_PREFIX + groupId + "/" + partition + "/" + key;
    }

    private void emit(TopologyEvent event) {
        sink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
