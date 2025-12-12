package com.ees.cluster.model;

/**
 * 리더 선출 방식.
 */
public enum LeaderElectionMode {
    KAFKA,
    RAFT,
    CAS
}
