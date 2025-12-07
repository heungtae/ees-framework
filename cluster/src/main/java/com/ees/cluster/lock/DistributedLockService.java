package com.ees.cluster.lock;

import com.ees.cluster.model.LockRecord;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface DistributedLockService {

    Mono<Optional<LockRecord>> tryAcquire(String lockName, String ownerNodeId, Duration leaseDuration, Map<String, String> metadata);

    Mono<Optional<LockRecord>> refresh(String lockName, String ownerNodeId, Duration leaseDuration);

    Mono<Boolean> release(String lockName, String ownerNodeId);

    Mono<Optional<LockRecord>> getLock(String lockName);
}
