package com.ees.cluster.lock;

import com.ees.cluster.model.LockRecord;
import com.ees.cluster.state.ClusterStateRepository;

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
    public Optional<LockRecord> tryAcquire(String lockName, String ownerNodeId, Duration leaseDuration, Map<String, String> metadata) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        Instant now = clock.instant();
        LockRecord desired = new LockRecord(lockName, ownerNodeId, now.plus(leaseDuration), metadata);
        Optional<LockRecord> optional = repository.get(lockKey(lockName), LockRecord.class);
        if (optional.isEmpty()) {
            boolean acquired = repository.putIfAbsent(lockKey(lockName), desired, leaseDuration);
            return acquired ? Optional.of(desired) : Optional.empty();
        }
        LockRecord current = optional.get();
        if (current.isExpired(now) || current.ownerNodeId().equals(ownerNodeId)) {
            boolean success = repository.compareAndSet(lockKey(lockName), current, desired, leaseDuration);
            return success ? Optional.of(desired) : Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public Optional<LockRecord> refresh(String lockName, String ownerNodeId, Duration leaseDuration) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Instant now = clock.instant();
        Optional<LockRecord> optional = repository.get(lockKey(lockName), LockRecord.class);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        LockRecord current = optional.get();
        if (!current.ownerNodeId().equals(ownerNodeId) || current.isExpired(now)) {
            return Optional.empty();
        }
        LockRecord refreshed = new LockRecord(lockName, ownerNodeId, now.plus(leaseDuration), current.metadata());
        boolean success = repository.compareAndSet(lockKey(lockName), current, refreshed, leaseDuration);
        return success ? Optional.of(refreshed) : Optional.empty();
    }

    @Override
    public boolean release(String lockName, String ownerNodeId) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        Optional<LockRecord> optional = repository.get(lockKey(lockName), LockRecord.class);
        if (optional.isEmpty()) {
            return false;
        }
        LockRecord current = optional.get();
        if (!current.ownerNodeId().equals(ownerNodeId)) {
            return false;
        }
        return repository.delete(lockKey(lockName));
    }

    @Override
    public Optional<LockRecord> getLock(String lockName) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        return repository.get(lockKey(lockName), LockRecord.class);
    }

    @Override
    public Map<String, LockRecord> snapshotLocks() {
        return repository.scan(LOCK_PREFIX, LockRecord.class).stream()
            .collect(java.util.stream.Collectors.toMap(record -> lockKey(record.name()), record -> record));
    }

    @Override
    public void restoreLocks(Map<String, LockRecord> locks) {
        Objects.requireNonNull(locks, "locks must not be null");
        Instant now = clock.instant();
        locks.forEach((key, record) -> {
            if (record.isExpired(now)) {
                return;
            }
            Duration ttl = Duration.between(now, record.leaseUntil());
            repository.put(key, record, ttl.isNegative() ? Duration.ZERO : ttl);
        });
    }

    private String lockKey(String lockName) {
        return LOCK_PREFIX + lockName;
    }
}
