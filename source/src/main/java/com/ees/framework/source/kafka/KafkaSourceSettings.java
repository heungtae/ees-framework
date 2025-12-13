package com.ees.framework.source.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * KafkaSource 생성에 필요한 설정 값 모음.
 * <p>
 * 프레임워크의 {@code Source<T>#read()} 모델에 맞추기 위해 {@link KafkaSource}는
 * {@link #pollTimeout()} 동안 poll 한 결과를 "한 번의 read 호출 결과"로 반환한다.
 */
public record KafkaSourceSettings(
    String bootstrapServers,
    List<String> topics,
    String groupId,
    String clientId,
    String commandName,
    String affinityKind,
    Duration pollTimeout,
    int maxPollRecords,
    boolean enableAutoCommit,
    String autoOffsetReset,
    String sourceId,
    Map<String, String> additionalProperties
) {

    public KafkaSourceSettings {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("bootstrapServers must not be blank");
        }
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId must not be blank");
        }
        Objects.requireNonNull(topics, "topics must not be null");
        Objects.requireNonNull(pollTimeout, "pollTimeout must not be null");
        if (autoOffsetReset == null || autoOffsetReset.isBlank()) {
            throw new IllegalArgumentException("autoOffsetReset must not be blank");
        }
        Objects.requireNonNull(additionalProperties, "additionalProperties must not be null");

        if (maxPollRecords <= 0) {
            throw new IllegalArgumentException("maxPollRecords must be > 0");
        }
        if (pollTimeout.isNegative() || pollTimeout.isZero()) {
            throw new IllegalArgumentException("pollTimeout must be > 0");
        }
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("topics must not be empty");
        }
    }
}
