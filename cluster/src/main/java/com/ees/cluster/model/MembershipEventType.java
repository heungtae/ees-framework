package com.ees.cluster.model;

/**
 * 멤버십 이벤트 타입.
 */
public enum MembershipEventType {
    JOINED,
    HEARTBEAT,
    SUSPECTED,
    DOWN,
    LEFT,
    REMOVED,
    UPDATED
}
