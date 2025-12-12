package com.ees.cluster.raft;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Guards processing while a rebalance or handoff is in progress.
 */
public class RebalanceSafeModeGuard {
    // AtomicBoolean 동작을 수행한다.

    private final AtomicBoolean safeMode = new AtomicBoolean(false);
    private final AtomicReference<String> reason = new AtomicReference<>("");
    private final AtomicReference<Instant> since = new AtomicReference<>();
    /**
     * enterSafeMode를 수행한다.
     * @param reason 
     */

    public void enterSafeMode(String reason) {
        this.safeMode.set(true);
        this.reason.set(reason == null ? "" : reason);
        this.since.set(Instant.now());
    }
    /**
     * exitSafeMode를 수행한다.
     */

    public void exitSafeMode() {
        this.safeMode.set(false);
        this.reason.set("");
        this.since.set(null);
    }
    /**
     * safeMode 여부를 반환한다.
     * @return 
     */

    public boolean isSafeMode() {
        return safeMode.get();
    }
    /**
     * reason를 수행한다.
     * @return 
     */

    public String reason() {
        return reason.get();
    }
    /**
     * since를 수행한다.
     * @return 
     */

    public Instant since() {
        return since.get();
    }
    /**
     * allowProcessing를 수행한다.
     * @return 
     */

    public ProcessingDecision allowProcessing() {
        if (safeMode.get()) {
            return ProcessingDecision.denied("safe-mode:" + reason.get());
        }
        return ProcessingDecision.allowed("active");
    }
}
