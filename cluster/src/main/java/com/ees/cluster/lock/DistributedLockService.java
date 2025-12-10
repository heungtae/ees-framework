package com.ees.cluster.lock;

import com.ees.cluster.model.LockRecord;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface DistributedLockService {

    Optional<LockRecord> tryAcquire(String lockName, String ownerNodeId, Duration leaseDuration, Map<String, String> metadata);

    Optional<LockRecord> refresh(String lockName, String ownerNodeId, Duration leaseDuration);

    boolean release(String lockName, String ownerNodeId);

    Optional<LockRecord> getLock(String lockName);

    /**
     * Snapshot view of all known locks. Default implementation returns an empty map for services that
     * do not retain lock state locally.
     */
    default Map<String, LockRecord> snapshotLocks() {
        return Map.of();
    }

    /**
     * Restore locks from a snapshot. Default implementation is a no-op for services that cannot
     * persist locks directly.
     */
    default void restoreLocks(Map<String, LockRecord> locks) {
        // no-op
    }
}
