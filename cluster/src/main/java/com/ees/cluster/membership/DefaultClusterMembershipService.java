package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.ClusterNodeStatus;
import com.ees.cluster.model.MembershipEvent;
import com.ees.cluster.model.MembershipEventType;
import com.ees.cluster.state.ClusterStateRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DefaultClusterMembershipService implements ClusterMembershipService {

    private static final String NODES_PREFIX = "cluster:nodes/";

    private final ClusterStateRepository repository;
    private final ClusterMembershipProperties properties;
    private final CopyOnWriteArrayList<Consumer<MembershipEvent>> listeners = new CopyOnWriteArrayList<>();
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
    public ClusterNodeRecord join(ClusterNode node) {
        Objects.requireNonNull(node, "node must not be null");
        Instant now = clock.instant();
        ClusterNodeRecord record = new ClusterNodeRecord(node, ClusterNodeStatus.UP, now, now);
        repository.put(nodeKey(node.nodeId()), record, ttl());
        emitEvent(new MembershipEvent(MembershipEventType.JOINED, record, now));
        return record;
    }

    @Override
    public ClusterNodeRecord heartbeat(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Instant now = clock.instant();
        Optional<ClusterNodeRecord> optional = repository.get(nodeKey(nodeId), ClusterNodeRecord.class);
        if (optional.isEmpty()) {
            throw new IllegalStateException("Node not found: " + nodeId);
        }
        return updateHeartbeat(optional.get(), now);
    }

    private ClusterNodeRecord updateHeartbeat(ClusterNodeRecord record, Instant heartbeatTime) {
        ClusterNodeStatus newStatus = record.status() == ClusterNodeStatus.LEFT
                ? ClusterNodeStatus.LEFT
                : ClusterNodeStatus.UP;
        ClusterNodeRecord updated = new ClusterNodeRecord(record.node(), newStatus, record.joinedAt(), heartbeatTime);
        repository.put(nodeKey(record.node().nodeId()), updated, ttl());
        emitEvent(new MembershipEvent(MembershipEventType.HEARTBEAT, updated, heartbeatTime));
        return updated;
    }

    @Override
    public void leave(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Instant now = clock.instant();
        Optional<ClusterNodeRecord> optional = repository.get(nodeKey(nodeId), ClusterNodeRecord.class);
        optional.ifPresent(record -> {
            ClusterNodeRecord updated = record.withStatus(ClusterNodeStatus.LEFT);
            repository.put(nodeKey(nodeId), updated, properties.heartbeatTimeout());
            emitEvent(new MembershipEvent(MembershipEventType.LEFT, updated, now));
        });
    }

    @Override
    public void remove(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Optional<ClusterNodeRecord> optional = repository.get(nodeKey(nodeId), ClusterNodeRecord.class);
        boolean deleted = repository.delete(nodeKey(nodeId));
        if (deleted) {
            optional.ifPresent(record -> emitEvent(new MembershipEvent(MembershipEventType.REMOVED, record, clock.instant())));
        }
    }

    @Override
    public Optional<ClusterNodeRecord> findNode(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        return repository.get(nodeKey(nodeId), ClusterNodeRecord.class);
    }

    @Override
    public Map<String, ClusterNodeRecord> view() {
        Map<String, ClusterNodeRecord> view = new ConcurrentHashMap<>();
        repository.scan(NODES_PREFIX, ClusterNodeRecord.class)
            .forEach(record -> view.put(record.node().nodeId(), record));
        return view;
    }

    @Override
    public void detectTimeouts() {
        Instant now = clock.instant();
        Duration timeout = properties.heartbeatTimeout();
        Duration downThreshold = timeout.multipliedBy(2);
        repository.scan(NODES_PREFIX, ClusterNodeRecord.class)
            .forEach(record -> evaluateRecord(record, now, timeout, downThreshold));
    }

    private void evaluateRecord(ClusterNodeRecord record, Instant now, Duration suspectAfter, Duration downAfter) {
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
            return;
        }

        ClusterNodeRecord updated = record.withStatus(nextStatus);
        MembershipEventType eventType = nextStatus == ClusterNodeStatus.SUSPECT
                ? MembershipEventType.SUSPECTED
                : MembershipEventType.DOWN;
        repository.put(nodeKey(record.node().nodeId()), updated, ttl());
        emitEvent(new MembershipEvent(eventType, updated, now));
    }

    @Override
    public void events(Consumer<MembershipEvent> consumer) {
        listeners.add(consumer);
    }

    private Duration ttl() {
        return properties.heartbeatTimeout().multipliedBy(2).plus(properties.heartbeatInterval());
    }

    private String nodeKey(String nodeId) {
        return NODES_PREFIX + nodeId;
    }

    private void emitEvent(MembershipEvent event) {
        for (Consumer<MembershipEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
