package com.ees.cluster.raft;

public record ProcessingDecision(
        boolean allowed,
        String reason
) {

    public static ProcessingDecision allowed(String reason) {
        return new ProcessingDecision(true, reason);
    }

    public static ProcessingDecision denied(String reason) {
        return new ProcessingDecision(false, reason);
    }
}
