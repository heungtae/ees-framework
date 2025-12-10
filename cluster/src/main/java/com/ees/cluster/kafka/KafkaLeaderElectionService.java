package com.ees.cluster.kafka;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simplified leader view for Kafka mode. Kafka's partition assignments determine
 * active nodes; this class exposes a group-level leader with lease semantics
 * to be used for non-Kafka coordinated tasks.
 */
public class KafkaLeaderElectionService implements LeaderElectionService {

    private final ConcurrentHashMap<String, LeaderInfo> leaders = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<LeaderInfo>> listeners = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public KafkaLeaderElectionService() {
        this(Clock.systemUTC());
    }

    public KafkaLeaderElectionService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<LeaderInfo> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Instant now = clock.instant();
        LeaderInfo current = leaders.get(groupId);
        long term = current == null ? 1L : current.term() + 1;
        LeaderInfo updated = new LeaderInfo(groupId, nodeId, mode, term, now, now.plus(leaseDuration));
        leaders.put(groupId, updated);
        emit(updated);
        return Optional.of(updated);
    }

    @Override
    public boolean release(String groupId, String nodeId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        LeaderInfo current = leaders.get(groupId);
        if (current != null && current.leaderNodeId().equals(nodeId)) {
            leaders.remove(groupId);
            return true;
        }
        return false;
    }

    @Override
    public Optional<LeaderInfo> getLeader(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        LeaderInfo current = leaders.get(groupId);
        if (current == null) {
            return Optional.empty();
        }
        if (isExpired(current)) {
            leaders.remove(groupId);
            return Optional.empty();
        }
        return Optional.of(current);
    }

    @Override
    public void watch(String groupId, Consumer<LeaderInfo> consumer) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        listeners.add(info -> {
            if (info.groupId().equals(groupId)) {
                consumer.accept(info);
            }
        });
    }

    private boolean isExpired(LeaderInfo info) {
        Instant now = clock.instant();
        return !info.leaseExpiresAt().isAfter(now);
    }

    private void emit(LeaderInfo info) {
        for (Consumer<LeaderInfo> listener : listeners) {
            listener.accept(info);
        }
    }
}
