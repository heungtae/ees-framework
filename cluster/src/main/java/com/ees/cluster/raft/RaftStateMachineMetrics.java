package com.ees.cluster.raft;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory metrics holder for a single Raft state machine/group.
 */
public class RaftStateMachineMetrics {
    // ofSeconds 동작을 수행한다.

    private static final Duration DEFAULT_STALENESS = Duration.ofSeconds(30);

    private final AtomicReference<String> groupId = new AtomicReference<>("unknown");
    // AtomicBoolean 동작을 수행한다.
    private final AtomicBoolean running = new AtomicBoolean(false);
    // AtomicLong 동작을 수행한다.
    private final AtomicLong lastAppliedIndex = new AtomicLong(-1L);
    // AtomicLong 동작을 수행한다.
    private final AtomicLong lastSnapshotIndex = new AtomicLong(-1L);
    // AtomicLong 동작을 수행한다.
    private final AtomicLong snapshotsTaken = new AtomicLong(0L);
    private final AtomicReference<String> leaderId = new AtomicReference<>("");
    private final AtomicReference<Instant> lastAppliedAt = new AtomicReference<>();
    private final AtomicReference<Instant> lastSnapshotAt = new AtomicReference<>();
    private final AtomicReference<Instant> startedAt = new AtomicReference<>();
    private final AtomicReference<Instant> stoppedAt = new AtomicReference<>();
    // AtomicBoolean 동작을 수행한다.
    private final AtomicBoolean safeMode = new AtomicBoolean(false);
    private final AtomicReference<String> safeModeReason = new AtomicReference<>("");
    /**
     * groupId를 설정한다.
     * @param groupId 
     */

    public void setGroupId(String groupId) {
        this.groupId.set(Objects.requireNonNull(groupId, "groupId must not be null"));
    }
    /**
     * markStarted를 수행한다.
     * @param now 
     */

    public void markStarted(Instant now) {
        running.set(true);
        startedAt.compareAndSet(null, now);
    }
    /**
     * markStopped를 수행한다.
     * @param now 
     */

    public void markStopped(Instant now) {
        running.set(false);
        stoppedAt.set(now);
    }
    /**
     * recordApply를 수행한다.
     * @param index 
     * @param when 
     */

    public void recordApply(long index, Instant when) {
        lastAppliedIndex.set(index);
        lastAppliedAt.set(when);
    }
    /**
     * recordSnapshot를 수행한다.
     * @param index 
     * @param when 
     */

    public void recordSnapshot(long index, Instant when) {
        lastSnapshotIndex.set(index);
        lastSnapshotAt.set(when);
        snapshotsTaken.incrementAndGet();
    }
    /**
     * updateLeader를 수행한다.
     * @param leader 
     */

    public void updateLeader(String leader) {
        leaderId.set(leader == null ? "" : leader);
    }
    /**
     * groupId를 수행한다.
     * @return 
     */

    public String groupId() {
        return groupId.get();
    }
    /**
     * lastAppliedIndex를 수행한다.
     * @return 
     */

    public long lastAppliedIndex() {
        return lastAppliedIndex.get();
    }
    /**
     * lastSnapshotIndex를 수행한다.
     * @return 
     */

    public long lastSnapshotIndex() {
        return lastSnapshotIndex.get();
    }
    /**
     * snapshotsTaken를 수행한다.
     * @return 
     */

    public long snapshotsTaken() {
        return snapshotsTaken.get();
    }
    /**
     * running를 수행한다.
     * @return 
     */

    public boolean running() {
        return running.get();
    }
    /**
     * safeMode를 설정한다.
     * @param enabled 
     * @param reason 
     */

    public void setSafeMode(boolean enabled, String reason) {
        safeMode.set(enabled);
        safeModeReason.set(reason == null ? "" : reason);
    }
    /**
     * safeMode를 수행한다.
     * @return 
     */

    public boolean safeMode() {
        return safeMode.get();
    }
    /**
     * safeModeReason를 수행한다.
     * @return 
     */

    public String safeModeReason() {
        return safeModeReason.get();
    }
    /**
     * health를 수행한다.
     * @param clock 
     * @return 
     */

    public RaftHealthSnapshot health(Clock clock) {
        return health(clock, DEFAULT_STALENESS);
    }
    /**
     * health를 수행한다.
     * @param clock 
     * @param stalenessThreshold 
     * @return 
     */

    public RaftHealthSnapshot health(Clock clock, Duration stalenessThreshold) {
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(stalenessThreshold, "stalenessThreshold must not be null");
        Instant lastApplied = lastAppliedAt.get();
        Instant lastSnapshot = lastSnapshotAt.get();
        Instant now = clock.instant();
        boolean stale = lastApplied != null && stalenessThreshold.toMillis() > 0
                && lastApplied.isBefore(now.minus(stalenessThreshold));
        return new RaftHealthSnapshot(
                groupId.get(),
                running.get(),
                leaderId.get(),
                lastAppliedIndex.get(),
                lastSnapshotIndex.get(),
                lastApplied,
                lastSnapshot,
                stale,
                safeMode.get(),
                safeModeReason.get()
        );
    }
}
