package com.ees.cluster.lock;

import com.ees.cluster.model.LockRecord;
import com.ees.cluster.state.ClusterStateRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DefaultDistributedLockService implements DistributedLockService {

    private static final String LOCK_PREFIX = "cluster:locks/";

    private final ClusterStateRepository repository;
    private final Clock clock;

    public DefaultDistributedLockService(ClusterStateRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public DefaultDistributedLockService(ClusterStateRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Mono<Optional<LockRecord>> tryAcquire(String lockName, String ownerNodeId, Duration leaseDuration, Map<String, String> metadata) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        Instant now = clock.instant();
        LockRecord desired = new LockRecord(lockName, ownerNodeId, now.plus(leaseDuration), metadata);
        return repository.get(lockKey(lockName), LockRecord.class)
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return repository.putIfAbsent(lockKey(lockName), desired, leaseDuration)
                                .map(acquired -> acquired ? Optional.of(desired) : Optional.empty());
                    }
                    LockRecord current = optional.get();
                    if (current.isExpired(now) || current.ownerNodeId().equals(ownerNodeId)) {
                        return repository.compareAndSet(lockKey(lockName), current, desired, leaseDuration)
                                .map(success -> success ? Optional.of(desired) : Optional.empty());
                    }
                    return Mono.just(Optional.empty());
                });
    }

    @Override
    public Mono<Optional<LockRecord>> refresh(String lockName, String ownerNodeId, Duration leaseDuration) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Instant now = clock.instant();
        return repository.get(lockKey(lockName), LockRecord.class)
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Mono.just(Optional.empty());
                    }
                    LockRecord current = optional.get();
                    if (!current.ownerNodeId().equals(ownerNodeId) || current.isExpired(now)) {
                        return Mono.just(Optional.empty());
                    }
                    LockRecord refreshed = new LockRecord(lockName, ownerNodeId, now.plus(leaseDuration), current.metadata());
                    return repository.compareAndSet(lockKey(lockName), current, refreshed, leaseDuration)
                            .map(success -> success ? Optional.of(refreshed) : Optional.empty());
                });
    }

    @Override
    public Mono<Boolean> release(String lockName, String ownerNodeId) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        return repository.get(lockKey(lockName), LockRecord.class)
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Mono.just(false);
                    }
                    LockRecord current = optional.get();
                    if (!current.ownerNodeId().equals(ownerNodeId)) {
                        return Mono.just(false);
                    }
                    return repository.delete(lockKey(lockName));
                });
    }

    @Override
    public Mono<Optional<LockRecord>> getLock(String lockName) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        return repository.get(lockKey(lockName), LockRecord.class);
    }

    private String lockKey(String lockName) {
        return LOCK_PREFIX + lockName;
    }
}
