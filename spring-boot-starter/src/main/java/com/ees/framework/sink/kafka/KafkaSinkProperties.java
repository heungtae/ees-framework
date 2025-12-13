package com.ees.framework.sink.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Sink 설정 프로퍼티.
 * <p>
 * {@code ees.sink.kafka.*} 프리픽스로 바인딩되며, {@link KafkaSink} 생성에 사용된다.
 */
@ConfigurationProperties(prefix = "ees.sink.kafka")
@Validated
public class KafkaSinkProperties {

    private boolean enabled = false;
    private String bootstrapServers;
    private String topic;
    private String clientId;
    private String acks = "all";
    private boolean enableIdempotence = false;
    private Duration sendTimeout = Duration.ofSeconds(5);
    private boolean synchronous = true;
    private String topicHeaderKey;
    private String keyHeaderKey;
    private boolean includeFxHeaders = true;
    private String sinkId = KafkaSink.SINK_TYPE;
    private Map<String, String> additionalProperties = new HashMap<>();

    /**
     * 현재 프로퍼티 값을 {@link KafkaSinkSettings}로 변환한다.
     *
     * @return KafkaSinkSettings
     */
    public KafkaSinkSettings toSettings() {
        return new KafkaSinkSettings(
            bootstrapServers,
            topic,
            clientId,
            acks,
            enableIdempotence,
            sendTimeout,
            synchronous,
            topicHeaderKey,
            keyHeaderKey,
            includeFxHeaders,
            sinkId,
            additionalProperties
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAcks() {
        return acks;
    }

    public void setAcks(String acks) {
        this.acks = acks;
    }

    public boolean isEnableIdempotence() {
        return enableIdempotence;
    }

    public void setEnableIdempotence(boolean enableIdempotence) {
        this.enableIdempotence = enableIdempotence;
    }

    public Duration getSendTimeout() {
        return sendTimeout;
    }

    public void setSendTimeout(Duration sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    public String getTopicHeaderKey() {
        return topicHeaderKey;
    }

    public void setTopicHeaderKey(String topicHeaderKey) {
        this.topicHeaderKey = topicHeaderKey;
    }

    public String getKeyHeaderKey() {
        return keyHeaderKey;
    }

    public void setKeyHeaderKey(String keyHeaderKey) {
        this.keyHeaderKey = keyHeaderKey;
    }

    public boolean isIncludeFxHeaders() {
        return includeFxHeaders;
    }

    public void setIncludeFxHeaders(boolean includeFxHeaders) {
        this.includeFxHeaders = includeFxHeaders;
    }

    public String getSinkId() {
        return sinkId;
    }

    public void setSinkId(String sinkId) {
        this.sinkId = sinkId;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}

