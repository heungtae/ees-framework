package com.ees.cluster.lock;

import com.ees.cluster.model.LockRecord;
import com.ees.cluster.state.InMemoryClusterStateRepository;
import com.ees.cluster.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDistributedLockServiceTest {

    private MutableClock clock;
    private DefaultDistributedLockService lockService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        lockService = new DefaultDistributedLockService(new InMemoryClusterStateRepository(clock), clock);
    }

    @Test
    void lockAcquisitionAndRefresh() {
        Optional<LockRecord> acquired = lockService.tryAcquire("job", "node-1", Duration.ofSeconds(5), Map.of());
        assertTrue(acquired.isPresent());

        Optional<LockRecord> blocked = lockService.tryAcquire("job", "node-2", Duration.ofSeconds(5), Map.of());
        assertTrue(blocked.isEmpty());

        clock.advance(Duration.ofSeconds(2));
        Optional<LockRecord> refreshed = lockService.refresh("job", "node-1", Duration.ofSeconds(5));
        assertTrue(refreshed.isPresent());

        clock.advance(Duration.ofSeconds(4));
        Optional<LockRecord> stillOwned = lockService.getLock("job");
        assertTrue(stillOwned.isPresent());
        assertTrue(stillOwned.get().ownerNodeId().equals("node-1"));
    }

    @Test
    void lockExpiresAndCanBeReacquired() {
        lockService.tryAcquire("job", "node-1", Duration.ofSeconds(3), Map.of());
        clock.advance(Duration.ofSeconds(4));
        Optional<LockRecord> reacquired = lockService.tryAcquire("job", "node-2", Duration.ofSeconds(3), Map.of());
        assertTrue(reacquired.isPresent());
        assertTrue(reacquired.get().ownerNodeId().equals("node-2"));
    }

    @Test
    void onlyOwnerCanRelease() {
        lockService.tryAcquire("job", "node-1", Duration.ofSeconds(3), Map.of());
        Boolean failed = lockService.release("job", "node-2");
        assertFalse(failed);
        Boolean success = lockService.release("job", "node-1");
        assertTrue(success);
    }
}
