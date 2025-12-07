package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.ClusterNodeStatus;
import com.ees.cluster.model.MembershipEvent;
import com.ees.cluster.model.MembershipEventType;
import com.ees.cluster.state.ClusterStateRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultClusterMembershipService implements ClusterMembershipService {

    private static final String NODES_PREFIX = "cluster:nodes/";

    private final ClusterStateRepository repository;
    private final ClusterMembershipProperties properties;
    private final Sinks.Many<MembershipEvent> eventSink =
            Sinks.many().multicast().onBackpressureBuffer();
    private final Clock clock;

    public DefaultClusterMembershipService(ClusterStateRepository repository,
                                           ClusterMembershipProperties properties) {
        this(repository, properties, Clock.systemUTC());
    }

    public DefaultClusterMembershipService(ClusterStateRepository repository,
                                           ClusterMembershipProperties properties,
                                           Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Mono<ClusterNodeRecord> join(ClusterNode node) {
        Objects.requireNonNull(node, "node must not be null");
        Instant now = clock.instant();
        ClusterNodeRecord record = new ClusterNodeRecord(node, ClusterNodeStatus.UP, now, now);
        return repository.put(nodeKey(node.nodeId()), record, ttl())
                .doOnSuccess(ignored -> emitEvent(new MembershipEvent(MembershipEventType.JOINED, record, now)))
                .thenReturn(record);
    }

    @Override
    public Mono<ClusterNodeRecord> heartbeat(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Instant now = clock.instant();
        return repository.get(nodeKey(nodeId), ClusterNodeRecord.class)
                .flatMap(optional -> optional
                        .map(record -> updateHeartbeat(record, now))
                        .orElseGet(() -> Mono.error(new IllegalStateException("Node not found: " + nodeId))));
    }

    private Mono<ClusterNodeRecord> updateHeartbeat(ClusterNodeRecord record, Instant heartbeatTime) {
        ClusterNodeStatus newStatus = record.status() == ClusterNodeStatus.LEFT
                ? ClusterNodeStatus.LEFT
                : ClusterNodeStatus.UP;
        ClusterNodeRecord updated = new ClusterNodeRecord(record.node(), newStatus, record.joinedAt(), heartbeatTime);
        return repository.put(nodeKey(record.node().nodeId()), updated, ttl())
                .doOnSuccess(ignored -> emitEvent(new MembershipEvent(MembershipEventType.HEARTBEAT, updated, heartbeatTime)))
                .thenReturn(updated);
    }

    @Override
    public Mono<Void> leave(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Instant now = clock.instant();
        return repository.get(nodeKey(nodeId), ClusterNodeRecord.class)
                .flatMap(optional -> optional.map(record -> {
                    ClusterNodeRecord updated = record.withStatus(ClusterNodeStatus.LEFT);
                    return repository.put(nodeKey(nodeId), updated, properties.heartbeatTimeout())
                            .doOnSuccess(ignored -> emitEvent(new MembershipEvent(MembershipEventType.LEFT, updated, now)))
                            .then();
                }).orElseGet(Mono::empty));
    }

    @Override
    public Mono<Void> remove(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        return repository.get(nodeKey(nodeId), ClusterNodeRecord.class)
                .flatMap(optional -> repository.delete(nodeKey(nodeId))
                        .doOnSuccess(ignored -> optional.ifPresent(record ->
                                emitEvent(new MembershipEvent(MembershipEventType.REMOVED, record, clock.instant()))))
                        .then());
    }

    @Override
    public Mono<Optional<ClusterNodeRecord>> findNode(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        return repository.get(nodeKey(nodeId), ClusterNodeRecord.class);
    }

    @Override
    public Mono<Map<String, ClusterNodeRecord>> view() {
        Map<String, ClusterNodeRecord> view = new ConcurrentHashMap<>();
        return repository.scan(NODES_PREFIX, ClusterNodeRecord.class)
                .doOnNext(record -> view.put(record.node().nodeId(), record))
                .then(Mono.just(view));
    }

    @Override
    public Mono<Void> detectTimeouts() {
        Instant now = clock.instant();
        Duration timeout = properties.heartbeatTimeout();
        Duration downThreshold = timeout.multipliedBy(2);
        return repository.scan(NODES_PREFIX, ClusterNodeRecord.class)
                .flatMap(record -> evaluateRecord(record, now, timeout, downThreshold))
                .then();
    }

    private Mono<Void> evaluateRecord(ClusterNodeRecord record, Instant now, Duration suspectAfter, Duration downAfter) {
        Instant last = record.lastHeartbeat();
        ClusterNodeStatus nextStatus = null;
        Instant suspectAt = last.plus(suspectAfter);
        Instant downAt = last.plus(downAfter);
        if (record.status() != ClusterNodeStatus.LEFT) {
            if (!now.isBefore(downAt)) {
                if (record.status() != ClusterNodeStatus.DOWN) {
                    nextStatus = ClusterNodeStatus.DOWN;
                }
            } else if (!now.isBefore(suspectAt) && record.status() == ClusterNodeStatus.UP) {
                nextStatus = ClusterNodeStatus.SUSPECT;
            }
        }

        if (nextStatus == null) {
            return Mono.empty();
        }

        ClusterNodeRecord updated = record.withStatus(nextStatus);
        MembershipEventType eventType = nextStatus == ClusterNodeStatus.SUSPECT
                ? MembershipEventType.SUSPECTED
                : MembershipEventType.DOWN;
        return repository.put(nodeKey(record.node().nodeId()), updated, ttl())
                .doOnSuccess(ignored -> emitEvent(new MembershipEvent(eventType, updated, now)))
                .then();
    }

    @Override
    public Flux<MembershipEvent> events() {
        return eventSink.asFlux();
    }

    private Duration ttl() {
        return properties.heartbeatTimeout().multipliedBy(2).plus(properties.heartbeatInterval());
    }

    private String nodeKey(String nodeId) {
        return NODES_PREFIX + nodeId;
    }

    private void emitEvent(MembershipEvent event) {
        eventSink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
