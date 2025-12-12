package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 토폴로지(할당) 변경 이벤트.
 *
 * @param type 이벤트 타입(널 불가)
 * @param assignment 파티션/그룹 단위 할당(옵션)
 * @param keyAssignment 키 단위 할당(옵션)
 * @param emittedAt 발생 시각(널 불가)
 */
public record TopologyEvent(
        TopologyEventType type,
        Assignment assignment,
        KeyAssignment keyAssignment,
        Instant emittedAt
) {

    public TopologyEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(emittedAt, "emittedAt must not be null");
    }
}
