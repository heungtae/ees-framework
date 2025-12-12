package com.ees.cluster.model;

/**
 * 토폴로지/할당 변경 이벤트 타입.
 */
public enum TopologyEventType {
    ASSIGNED,
    REVOKED,
    UPDATED,
    KEY_ASSIGNED,
    KEY_UNASSIGNED
}
