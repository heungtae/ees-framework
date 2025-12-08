package com.ees.cluster.raft.command;

public enum CommandType {
    LOCK_ACQUIRE,
    LOCK_RELEASE,
    ASSIGN_PARTITION,
    REVOKE_PARTITION,
    ASSIGN_KEY,
    UNASSIGN_KEY
}
