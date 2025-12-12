package com.ees.framework.context;

import java.time.Instant;
import java.util.Objects;

/**
 * 소스에서 전달되는 메시지 페이로드와 기본 정보를 포장한다.
 *
 * @param sourceType 메시지의 Source 타입(예: {@code "kafka-order"})
 * @param payload 실제 페이로드(널 불가)
 * @param timestamp 생성 시각(널 불가)
 * @param key 라우팅/affinity 계산 등에 사용할 수 있는 키(옵션)
 */
public record FxMessage<T>(
    String sourceType,
    T payload,
    Instant timestamp,
    String key
) {
    public FxMessage {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static <T> FxMessage<T> now(String sourceType, T payload) {
        return new FxMessage<>(sourceType, payload, Instant.now(), null);
    }
}
