package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 멤버십 변경 이벤트.
 *
 * @param type 이벤트 타입(널 불가)
 * @param node 대상 노드 레코드(널 불가)
 * @param emittedAt 발생 시각(널 불가)
 */
public record MembershipEvent(
        MembershipEventType type,
        ClusterNodeRecord node,
        Instant emittedAt
) {

    public MembershipEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(emittedAt, "emittedAt must not be null");
    }
}
