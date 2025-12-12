package com.ees.cluster.raft;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import com.ees.cluster.state.ClusterStateEvent;
import com.ees.cluster.state.ClusterStateEventType;
import com.ees.cluster.state.ClusterStateRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Repository-backed leader tracking to emulate Ratis-driven leadership.
 * The actual Ratis integration should replace the CAS usage here with
 * log-committed state machine updates.
 */
public class RaftLeaderElectionService implements LeaderElectionService {

    private static final String LEADER_PREFIX = "cluster:raft/leader/";

    private final ClusterStateRepository repository;
    private final Clock clock;
    private final CopyOnWriteArrayList<Consumer<LeaderInfo>> listeners = new CopyOnWriteArrayList<>();
    /**
     * 인스턴스를 생성한다.
     * @param repository 
     */

    public RaftLeaderElectionService(ClusterStateRepository repository) {
        this(repository, Clock.systemUTC());
    }
    /**
     * 인스턴스를 생성한다.
     * @param repository 
     * @param clock 
     */

    public RaftLeaderElectionService(ClusterStateRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
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
        Optional<LeaderInfo> optional = repository.get(leaderKey(groupId), LeaderInfo.class);
        long term = optional.map(info -> info.term() + 1).orElse(1L);
        LeaderInfo desired = new LeaderInfo(groupId, nodeId, mode, term, now, now.plus(leaseDuration));
        if (optional.isEmpty()) {
            if (repository.putIfAbsent(leaderKey(groupId), desired, leaseDuration)) {
                emit(desired);
                return Optional.of(desired);
            }
            return Optional.empty();
        }
        LeaderInfo current = optional.get();
        if (isExpired(current, now) || current.leaderNodeId().equals(nodeId)) {
            boolean success = repository.compareAndSet(leaderKey(groupId), current, desired, leaseDuration);
            if (success) {
                emit(desired);
                return Optional.of(desired);
            }
        }
        return Optional.empty();
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
        Optional<LeaderInfo> optional = repository.get(leaderKey(groupId), LeaderInfo.class);
        if (optional.isEmpty() || !optional.get().leaderNodeId().equals(nodeId)) {
            return false;
        }
        repository.delete(leaderKey(groupId));
        return true;
    }
    /**
     * leader를 반환한다.
     * @param groupId 
     * @return 
     */

    @Override
    public Optional<LeaderInfo> getLeader(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Instant now = clock.instant();
        Optional<LeaderInfo> optional = repository.get(leaderKey(groupId), LeaderInfo.class);
        if (optional.isPresent() && isExpired(optional.get(), now)) {
            repository.delete(leaderKey(groupId));
            return Optional.empty();
        }
        return optional;
    }
    /**
     * watch를 수행한다.
     * @param groupId 
     * @param consumer 
     */

    @Override
    public void watch(String groupId, Consumer<LeaderInfo> consumer) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        String key = leaderKey(groupId);
        listeners.add(info -> {
            if (info.groupId().equals(groupId)) {
                consumer.accept(info);
            }
        });
        repository.watch(key, event -> {
            if (event.type() == ClusterStateEventType.PUT) {
                event.value()
                    .filter(LeaderInfo.class::isInstance)
                    .map(LeaderInfo.class::cast)
                    .ifPresent(consumer);
            }
        });
    }
    // leaderKey 동작을 수행한다.

    private String leaderKey(String groupId) {
        return LEADER_PREFIX + groupId;
    }
    // expired 여부를 반환한다.

    private boolean isExpired(LeaderInfo info, Instant now) {
        return !info.leaseExpiresAt().isAfter(now);
    }
    // emit 동작을 수행한다.

    private void emit(LeaderInfo info) {
        for (Consumer<LeaderInfo> listener : listeners) {
            listener.accept(info);
        }
    }
}
