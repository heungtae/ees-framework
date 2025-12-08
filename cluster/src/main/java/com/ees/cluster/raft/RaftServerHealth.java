package com.ees.cluster.raft;

public record RaftServerHealth(
        boolean running,
        int registeredGroups,
        int registeredStateMachines
) {
}
