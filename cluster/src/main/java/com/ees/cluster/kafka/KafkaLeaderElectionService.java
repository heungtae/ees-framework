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
    /**
     * KafkaLeaderElectionService를 수행한다.
     * @return 
     */

    public KafkaLeaderElectionService() {
        this(Clock.systemUTC());
    }
    /**
     * KafkaLeaderElectionService를 수행한다.
     * @param clock 
     * @return 
     */

    public KafkaLeaderElectionService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }
    /**
     * tryAcquireLeader를 수행한다.
     * @param groupId 
     * @param nodeId 
     * @param mode 
     * @param leaseDuration 
     * @return 
     */

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
    /**
     * release를 수행한다.
     * @param groupId 
     * @param nodeId 
     * @return 
     */

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
    /**
     * leader를 반환한다.
     * @param groupId 
     * @return 
     */

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
    /**
     * watch를 수행한다.
     * @param groupId 
     * @param consumer 
     */

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
    // expired 여부를 반환한다.

    private boolean isExpired(LeaderInfo info) {
        Instant now = clock.instant();
        return !info.leaseExpiresAt().isAfter(now);
    }
    // emit 동작을 수행한다.

    private void emit(LeaderInfo info) {
        for (Consumer<LeaderInfo> listener : listeners) {
            listener.accept(info);
        }
    }
}
