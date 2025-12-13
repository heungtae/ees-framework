package com.ees.framework.source.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka Source 설정 프로퍼티.
 * <p>
 * {@code ees.source.kafka.*} 프리픽스로 바인딩되며, {@link KafkaSource} 생성에 사용된다.
 */
@ConfigurationProperties(prefix = "ees.source.kafka")
@Validated
public class KafkaSourceProperties {

    private boolean enabled = false;
    private String bootstrapServers;
    private List<String> topics = new ArrayList<>();
    private String groupId;
    private String clientId;
    private String commandName = KafkaSource.SOURCE_TYPE;
    private String affinityKind = "equipmentId";
    private Duration pollTimeout = Duration.ofMillis(200);
    private int maxPollRecords = 200;
    private boolean enableAutoCommit = true;
    private String autoOffsetReset = "earliest";
    private String sourceId = KafkaSource.SOURCE_TYPE;
    private Map<String, String> additionalProperties = new HashMap<>();

    /**
     * 현재 프로퍼티 값을 {@link KafkaSourceSettings}로 변환한다.
     *
     * @return KafkaSourceSettings
     */
    public KafkaSourceSettings toSettings() {
        return new KafkaSourceSettings(
            bootstrapServers,
            topics,
            groupId,
            clientId,
            commandName,
            affinityKind,
            pollTimeout,
            maxPollRecords,
            enableAutoCommit,
            autoOffsetReset,
            sourceId,
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

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getAffinityKind() {
        return affinityKind;
    }

    public void setAffinityKind(String affinityKind) {
        this.affinityKind = affinityKind;
    }

    public Duration getPollTimeout() {
        return pollTimeout;
    }

    public void setPollTimeout(Duration pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public int getMaxPollRecords() {
        return maxPollRecords;
    }

    public void setMaxPollRecords(int maxPollRecords) {
        this.maxPollRecords = maxPollRecords;
    }

    public boolean isEnableAutoCommit() {
        return enableAutoCommit;
    }

    public void setEnableAutoCommit(boolean enableAutoCommit) {
        this.enableAutoCommit = enableAutoCommit;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public void setAutoOffsetReset(String autoOffsetReset) {
        this.autoOffsetReset = autoOffsetReset;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}

