package com.ees.framework.sink.kafka;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * {@link KafkaSink} 생성에 필요한 설정 값 모음.
 * <p>
 * {@link KafkaSink}는 {@link com.ees.framework.sink.Sink#write(com.ees.framework.context.FxContext)} 호출마다
 * 1건의 Kafka 메시지를 전송한다.
 */
public record KafkaSinkSettings(
    String bootstrapServers,
    String topic,
    String clientId,
    String acks,
    boolean enableIdempotence,
    Duration sendTimeout,
    boolean synchronous,
    String topicHeaderKey,
    String keyHeaderKey,
    boolean includeFxHeaders,
    String sinkId,
    Map<String, String> additionalProperties
) {

    public KafkaSinkSettings {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("bootstrapServers must not be blank");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        if (acks == null || acks.isBlank()) {
            throw new IllegalArgumentException("acks must not be blank");
        }
        Objects.requireNonNull(sendTimeout, "sendTimeout must not be null");
        Objects.requireNonNull(additionalProperties, "additionalProperties must not be null");
        if (sendTimeout.isNegative() || sendTimeout.isZero()) {
            throw new IllegalArgumentException("sendTimeout must be > 0");
        }
    }
}

