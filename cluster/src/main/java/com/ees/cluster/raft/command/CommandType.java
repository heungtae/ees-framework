package com.ees.cluster.raft.command;

/**
 * Raft 로그에 기록되는 명령 종류.
 */
public enum CommandType {
    LOCK_ACQUIRE,
    LOCK_RELEASE,
    ASSIGN_PARTITION,
    REVOKE_PARTITION,
    ASSIGN_KEY,
    UNASSIGN_KEY
}
