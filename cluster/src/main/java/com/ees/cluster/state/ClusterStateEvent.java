package com.ees.cluster.state;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link ClusterStateRepository}에서 발생한 변경 이벤트.
 *
 * @param key 변경된 키(널 불가)
 * @param type 이벤트 타입(널 불가)
 * @param value 이벤트 시점의 값(삭제/만료 등으로 없을 수 있음)
 * @param emittedAt 이벤트 발생 시각(널 불가)
 */
public record ClusterStateEvent(
        String key,
        ClusterStateEventType type,
        Optional<Object> value,
        Instant emittedAt
) {

    public ClusterStateEvent {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(emittedAt, "emittedAt must not be null");
    }
}
