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

    private static final Duration DEFAULT_STALENESS = Duration.ofSeconds(30);

    private final AtomicReference<String> groupId = new AtomicReference<>("unknown");
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastAppliedIndex = new AtomicLong(-1L);
    private final AtomicLong lastSnapshotIndex = new AtomicLong(-1L);
    private final AtomicLong snapshotsTaken = new AtomicLong(0L);
    private final AtomicReference<String> leaderId = new AtomicReference<>("");
    private final AtomicReference<Instant> lastAppliedAt = new AtomicReference<>();
    private final AtomicReference<Instant> lastSnapshotAt = new AtomicReference<>();
    private final AtomicReference<Instant> startedAt = new AtomicReference<>();
    private final AtomicReference<Instant> stoppedAt = new AtomicReference<>();
    private final AtomicBoolean safeMode = new AtomicBoolean(false);
    private final AtomicReference<String> safeModeReason = new AtomicReference<>("");

    public void setGroupId(String groupId) {
        this.groupId.set(Objects.requireNonNull(groupId, "groupId must not be null"));
    }

    public void markStarted(Instant now) {
        running.set(true);
        startedAt.compareAndSet(null, now);
    }

    public void markStopped(Instant now) {
        running.set(false);
        stoppedAt.set(now);
    }

    public void recordApply(long index, Instant when) {
        lastAppliedIndex.set(index);
        lastAppliedAt.set(when);
    }

    public void recordSnapshot(long index, Instant when) {
        lastSnapshotIndex.set(index);
        lastSnapshotAt.set(when);
        snapshotsTaken.incrementAndGet();
    }

    public void updateLeader(String leader) {
        leaderId.set(leader == null ? "" : leader);
    }

    public String groupId() {
        return groupId.get();
    }

    public long lastAppliedIndex() {
        return lastAppliedIndex.get();
    }

    public long lastSnapshotIndex() {
        return lastSnapshotIndex.get();
    }

    public long snapshotsTaken() {
        return snapshotsTaken.get();
    }

    public boolean running() {
        return running.get();
    }

    public void setSafeMode(boolean enabled, String reason) {
        safeMode.set(enabled);
        safeModeReason.set(reason == null ? "" : reason);
    }

    public boolean safeMode() {
        return safeMode.get();
    }

    public String safeModeReason() {
        return safeModeReason.get();
    }

    public RaftHealthSnapshot health(Clock clock) {
        return health(clock, DEFAULT_STALENESS);
    }

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
