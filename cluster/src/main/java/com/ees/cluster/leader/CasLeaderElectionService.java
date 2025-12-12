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

/**
 * {@link ClusterStateRepository}를 이용해 CAS 기반으로 리더 선출을 수행하는 구현.
 */
public class CasLeaderElectionService implements LeaderElectionService {

    private static final String LEADER_PREFIX = "cluster:leader/";

    private final ClusterStateRepository repository;
    private final Clock clock;

    /**
     * 시스템 UTC 시계를 사용해 생성한다.
     */
    public CasLeaderElectionService(ClusterStateRepository repository) {
        this(repository, Clock.systemUTC());
    }

    /**
     * 저장소/시계를 지정해 생성한다.
     *
     * @param repository 상태 저장소(널 불가)
     * @param clock 시간 소스(널 불가)
     */
    public CasLeaderElectionService(ClusterStateRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
    // leaderKey 동작을 수행한다.

    private String leaderKey(String groupId) {
        // 그룹별 리더 키를 구성한다.
        return LEADER_PREFIX + groupId;
    }
    // expired 여부를 반환한다.

    private boolean isExpired(LeaderInfo info, Instant now) {
        // 리스 만료 시각과 비교해 만료 여부를 판단한다.
        return info.leaseExpiresAt().isBefore(now) || info.leaseExpiresAt().equals(now);
    }
}
