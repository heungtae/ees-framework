package com.ees.cluster.leader;

import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import com.ees.cluster.state.ClusterStateEvent;
import com.ees.cluster.state.ClusterStateEventType;
import com.ees.cluster.state.ClusterStateRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

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
    public Mono<Optional<LeaderInfo>> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Instant now = clock.instant();
        return repository.get(leaderKey(groupId), LeaderInfo.class)
                .flatMap(optional -> {
                    long term = optional.map(info -> info.term() + 1).orElse(1L);
                    LeaderInfo desired = new LeaderInfo(groupId, nodeId, mode, term, now, now.plus(leaseDuration));
                    if (optional.isEmpty()) {
                        return repository.putIfAbsent(leaderKey(groupId), desired, leaseDuration)
                                .map(acquired -> acquired ? Optional.of(desired) : Optional.empty());
                    }
                    LeaderInfo current = optional.get();
                    if (isExpired(current, now) || current.leaderNodeId().equals(nodeId)) {
                        return repository.compareAndSet(leaderKey(groupId), current, desired, leaseDuration)
                                .map(success -> success ? Optional.of(desired) : Optional.empty());
                    }
                    return Mono.just(Optional.empty());
                });
    }

    @Override
    public Mono<Boolean> release(String groupId, String nodeId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        return repository.get(leaderKey(groupId), LeaderInfo.class)
                .flatMap(optional -> optional
                        .map(current -> {
                            if (!current.leaderNodeId().equals(nodeId)) {
                                return Mono.just(false);
                            }
                            return repository.delete(leaderKey(groupId));
                        })
                        .orElseGet(() -> Mono.just(false)));
    }

    @Override
    public Mono<Optional<LeaderInfo>> getLeader(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Instant now = clock.instant();
        return repository.get(leaderKey(groupId), LeaderInfo.class)
                .flatMap(optional -> {
                    if (optional.isPresent() && isExpired(optional.get(), now)) {
                        return repository.delete(leaderKey(groupId)).thenReturn(Optional.empty());
                    }
                    return Mono.just(optional);
                });
    }

    @Override
    public Flux<LeaderInfo> watch(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        String key = leaderKey(groupId);
        return repository.watch(key)
                .filter(event -> event.type() == ClusterStateEventType.PUT)
                .map(ClusterStateEvent::value)
                .flatMap(optional -> optional.map(Mono::just).orElseGet(Mono::empty))
                .filter(LeaderInfo.class::isInstance)
                .map(LeaderInfo.class::cast);
    }

    private String leaderKey(String groupId) {
        return LEADER_PREFIX + groupId;
    }

    private boolean isExpired(LeaderInfo info, Instant now) {
        return info.leaseExpiresAt().isBefore(now) || info.leaseExpiresAt().equals(now);
    }
}
