package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 멤버십 저장/전파에 사용하는 노드 레코드.
 *
 * @param node 노드 정보(널 불가)
 * @param status 노드 상태(널 불가)
 * @param joinedAt 조인 시각(널 불가)
 * @param lastHeartbeat 마지막 하트비트 시각(널 불가)
 */
public record ClusterNodeRecord(
        ClusterNode node,
        ClusterNodeStatus status,
        Instant joinedAt,
        Instant lastHeartbeat
) {

    public ClusterNodeRecord {
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        Objects.requireNonNull(lastHeartbeat, "lastHeartbeat must not be null");
    }

    /**
     * 상태를 교체한 새 레코드를 반환한다.
     *
     * @param newStatus 새 상태(널 불가)
     * @return 갱신된 레코드
     */
    public ClusterNodeRecord withStatus(ClusterNodeStatus newStatus) {
        return new ClusterNodeRecord(node, newStatus, joinedAt, lastHeartbeat);
    }

    /**
     * 마지막 하트비트 시각을 교체한 새 레코드를 반환한다.
     *
     * @param heartbeat 새 하트비트 시각(널 불가)
     * @return 갱신된 레코드
     */
    public ClusterNodeRecord withLastHeartbeat(Instant heartbeat) {
        return new ClusterNodeRecord(node, status, joinedAt, heartbeat);
    }
}
