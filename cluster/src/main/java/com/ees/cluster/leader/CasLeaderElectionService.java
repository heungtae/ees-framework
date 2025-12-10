package com.ees.cluster.leader;

import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import com.ees.cluster.state.ClusterStateRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class CasLeaderElectionService implements LeaderElectionService {

    private static final String LEADER_PREFIX = "cluster:leader/";

    private final ClusterStateRepository repository;
    private final Clock clock;

    public CasLeaderElectionService(ClusterStateRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public CasLeaderElectionService(ClusterStateRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

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
            boolean acquired = repository.putIfAbsent(leaderKey(groupId), desired, leaseDuration);
            return acquired ? Optional.of(desired) : Optional.empty();
        }
        LeaderInfo current = optional.get();
        if (isExpired(current, now) || current.leaderNodeId().equals(nodeId)) {
            boolean success = repository.compareAndSet(leaderKey(groupId), current, desired, leaseDuration);
            return success ? Optional.of(desired) : Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public boolean release(String groupId, String nodeId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Optional<LeaderInfo> optional = repository.get(leaderKey(groupId), LeaderInfo.class);
        if (optional.isPresent()) {
            LeaderInfo current = optional.get();
            if (!current.leaderNodeId().equals(nodeId)) {
                return false;
            }
            return repository.delete(leaderKey(groupId));
        }
        return false;
    }

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

    @Override
    public void watch(String groupId, Consumer<LeaderInfo> consumer) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        String key = leaderKey(groupId);
        repository.watch(key, event -> event.value().ifPresent(value -> {
            if (value instanceof LeaderInfo info) {
                consumer.accept(info);
            }
        }));
    }

    private String leaderKey(String groupId) {
        return LEADER_PREFIX + groupId;
    }

    private boolean isExpired(LeaderInfo info, Instant now) {
        return info.leaseExpiresAt().isBefore(now) || info.leaseExpiresAt().equals(now);
    }
}
