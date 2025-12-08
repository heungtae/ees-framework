package com.ees.cluster.raft;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Guards processing while a rebalance or handoff is in progress.
 */
public class RebalanceSafeModeGuard {

    private final AtomicBoolean safeMode = new AtomicBoolean(false);
    private final AtomicReference<String> reason = new AtomicReference<>("");
    private final AtomicReference<Instant> since = new AtomicReference<>();

    public void enterSafeMode(String reason) {
        this.safeMode.set(true);
        this.reason.set(reason == null ? "" : reason);
        this.since.set(Instant.now());
    }

    public void exitSafeMode() {
        this.safeMode.set(false);
        this.reason.set("");
        this.since.set(null);
    }

    public boolean isSafeMode() {
        return safeMode.get();
    }

    public String reason() {
        return reason.get();
    }

    public Instant since() {
        return since.get();
    }

    public ProcessingDecision allowProcessing() {
        if (safeMode.get()) {
            return ProcessingDecision.denied("safe-mode:" + reason.get());
        }
        return ProcessingDecision.allowed("active");
    }
}
