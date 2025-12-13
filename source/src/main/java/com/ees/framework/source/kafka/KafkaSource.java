package com.ees.framework.source.kafka;

import com.ees.framework.annotations.FxSource;
import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.source.Source;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Kafka 토픽에서 레코드를 poll 하여 {@link FxContext} 배치로 변환하는 {@link Source} 구현.
 * <p>
 * - payload: Kafka value(String)
 * - message.key: Kafka record key(String)
 * - affinity: 기본적으로 record key를 사용(키가 없으면 파티션 번호 문자열로 대체)
 * <p>
 * 주의: 현재 프레임워크는 처리 성공/실패에 따른 ack 모델이 없으므로, 기본 구현은
 * {@code enable.auto.commit=true} 전제를 권장한다.
 */
@FxSource(type = KafkaSource.SOURCE_TYPE)
public final class KafkaSource implements Source<String>, AutoCloseable {

    public static final String SOURCE_TYPE = "kafka";

    public static final String HEADER_AFFINITY_KIND = "affinity-kind";
    public static final String HEADER_AFFINITY_VALUE = "affinity-value";

    public static final String HEADER_KAFKA_TOPIC = "kafka-topic";
    public static final String HEADER_KAFKA_PARTITION = "kafka-partition";
    public static final String HEADER_KAFKA_OFFSET = "kafka-offset";

    private static final Logger log = LoggerFactory.getLogger(KafkaSource.class);

    private final KafkaSourceSettings settings;
    private final FxCommand command;

    private volatile Consumer<String, String> consumer;

    /**
     * 설정 기반으로 KafkaSource를 생성한다.
     *
     * @param settings Kafka 연결/구독/affinity 설정
     */
    public KafkaSource(KafkaSourceSettings settings) {
        this(settings, null);
    }

    KafkaSource(KafkaSourceSettings settings, Consumer<String, String> consumer) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.command = FxCommand.of(
            settings.commandName() != null && !settings.commandName().isBlank() ? settings.commandName() : SOURCE_TYPE
        );
        this.consumer = consumer;
    }

    /**
     * Kafka에서 {@link KafkaSourceSettings#pollTimeout()} 동안 poll 한 결과를 FxContext 리스트로 반환한다.
     *
     * @return 이번 poll 사이클에서 수신한 FxContext 목록(없으면 빈 리스트)
     */
    @Override
    public Iterable<FxContext<String>> read() {
        Consumer<String, String> consumer = ensureConsumer();
        ConsumerRecords<String, String> records = consumer.poll(settings.pollTimeout());
        if (records.isEmpty()) {
            return List.of();
        }
        List<FxContext<String>> batch = new ArrayList<>(records.count());
        for (ConsumerRecord<String, String> record : records) {
            if (record.value() == null) {
                continue;
            }
            batch.add(toContext(record));
        }
        return batch;
    }

    /**
     * 생성된 Kafka consumer 리소스를 정리한다.
     */
    @Override
    public void close() {
        Consumer<String, String> consumer = this.consumer;
        if (consumer == null) {
            return;
        }
        try {
            consumer.close();
        } catch (RuntimeException e) {
            log.warn("Failed to close Kafka consumer for sourceType={}", SOURCE_TYPE, e);
        } finally {
            this.consumer = null;
        }
    }

    private Consumer<String, String> ensureConsumer() {
        Consumer<String, String> current = consumer;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (consumer != null) {
                return consumer;
            }
            Consumer<String, String> created = new KafkaConsumer<>(buildConsumerProperties());
            created.subscribe(settings.topics());
            this.consumer = created;
            return created;
        }
    }

    private Properties buildConsumerProperties() {
        Properties props = new Properties();

        if (settings.bootstrapServers() != null) {
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, settings.bootstrapServers());
        }
        if (settings.groupId() != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, settings.groupId());
        }
        if (settings.clientId() != null) {
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, settings.clientId());
        }

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(settings.enableAutoCommit()));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, settings.autoOffsetReset());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.toString(settings.maxPollRecords()));

        settings.additionalProperties().forEach(props::putIfAbsent);
        return props;
    }

    private FxContext<String> toContext(ConsumerRecord<String, String> record) {
        String affinityKind = settings.affinityKind() != null ? settings.affinityKind() : "equipmentId";
        String affinityValue = record.key() != null ? record.key() : Integer.toString(record.partition());

        FxHeaders headers = FxHeaders.empty()
            .with(HEADER_AFFINITY_KIND, affinityKind)
            .with(HEADER_AFFINITY_VALUE, affinityValue)
            .with(HEADER_KAFKA_TOPIC, record.topic())
            .with(HEADER_KAFKA_PARTITION, Integer.toString(record.partition()))
            .with(HEADER_KAFKA_OFFSET, Long.toString(record.offset()));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("kafka.topic", record.topic());
        attributes.put("kafka.partition", record.partition());
        attributes.put("kafka.offset", record.offset());
        attributes.put("kafka.timestamp", record.timestamp());
        if (record.key() != null) {
            attributes.put("kafka.key", record.key());
        }

        FxMeta meta = new FxMeta(settings.sourceId(), null, 0, attributes);

        Instant timestamp = record.timestamp() >= 0 ? Instant.ofEpochMilli(record.timestamp()) : Instant.now();
        FxMessage<String> message = new FxMessage<>(SOURCE_TYPE, record.value(), timestamp, record.key());

        return new FxContext<>(
            command,
            headers,
            message,
            meta,
            FxAffinity.of(affinityKind, affinityValue)
        );
    }
}
