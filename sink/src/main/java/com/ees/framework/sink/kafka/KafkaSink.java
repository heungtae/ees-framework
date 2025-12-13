package com.ees.framework.sink.kafka;

import com.ees.framework.annotations.FxSink;
import com.ees.framework.context.FxContext;
import com.ees.framework.sink.Sink;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link FxContext}의 payload(String)를 Kafka 토픽으로 전송하는 {@link Sink} 구현.
 * <p>
 * - topic: 설정된 기본 토픽 또는 {@link KafkaSinkSettings#topicHeaderKey()} 헤더 값
 * - key: {@code keyHeaderKey -> message.key -> affinity.value} 우선순위로 결정
 * - headers: (옵션) FxHeaders를 Kafka record headers로 복사
 */
@FxSink(KafkaSink.SINK_TYPE)
public final class KafkaSink implements Sink<String>, AutoCloseable {

    public static final String SINK_TYPE = "kafka";

    public static final String HEADER_FX_COMMAND = "fx-command";
    public static final String HEADER_FX_AFFINITY_KIND = "fx-affinity-kind";
    public static final String HEADER_FX_AFFINITY_VALUE = "fx-affinity-value";
    public static final String HEADER_FX_MESSAGE_KEY = "fx-message-key";

    private static final Logger log = LoggerFactory.getLogger(KafkaSink.class);

    private final KafkaSinkSettings settings;
    private volatile Producer<String, String> producer;

    /**
     * 설정 기반으로 KafkaSink를 생성한다.
     *
     * @param settings Kafka 전송 설정
     */
    public KafkaSink(KafkaSinkSettings settings) {
        this(settings, null);
    }

    KafkaSink(KafkaSinkSettings settings, Producer<String, String> producer) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.producer = producer;
    }

    /**
     * 컨텍스트의 payload를 Kafka로 전송한다.
     * <p>
     * {@link KafkaSinkSettings#synchronous()}가 true이면 send 결과를 {@link KafkaSinkSettings#sendTimeout()}
     * 까지 대기한다.
     *
     * @param context 전송할 컨텍스트
     */
    @Override
    public void write(FxContext<String> context) {
        if (context == null) {
            return;
        }
        String payload = context.message().payload();
        if (payload == null) {
            return;
        }

        String topic = resolveTopic(context);
        String key = resolveKey(context);

        ProducerRecord<String, String> record = new ProducerRecord<>(
            topic,
            null,
            context.message().timestamp() != null ? context.message().timestamp().toEpochMilli() : null,
            key,
            payload,
            buildHeaders(context, key)
        );

        Producer<String, String> producer = ensureProducer();
        if (!settings.synchronous()) {
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.warn("Failed to send Kafka record topic={} sinkId={}", topic, settings.sinkId(), exception);
                }
            });
            return;
        }

        try {
            producer.send(record).get(settings.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka send interrupted", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Kafka send timed out after " + settings.sendTimeout(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Kafka send failed", e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * 생성된 Kafka producer 리소스를 정리한다.
     */
    @Override
    public void close() {
        Producer<String, String> producer = this.producer;
        if (producer == null) {
            return;
        }
        try {
            producer.flush();
            producer.close();
        } catch (RuntimeException e) {
            log.warn("Failed to close Kafka producer for sinkType={}", SINK_TYPE, e);
        } finally {
            this.producer = null;
        }
    }

    private Producer<String, String> ensureProducer() {
        Producer<String, String> current = producer;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (producer != null) {
                return producer;
            }
            Producer<String, String> created = new KafkaProducer<>(buildProducerProperties());
            this.producer = created;
            return created;
        }
    }

    private Properties buildProducerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, settings.bootstrapServers());
        if (settings.clientId() != null && !settings.clientId().isBlank()) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, settings.clientId());
        }
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        props.put(ProducerConfig.ACKS_CONFIG, settings.acks());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, Boolean.toString(settings.enableIdempotence()));

        settings.additionalProperties().forEach(props::putIfAbsent);
        return props;
    }

    private String resolveTopic(FxContext<String> context) {
        if (settings.topicHeaderKey() == null || settings.topicHeaderKey().isBlank()) {
            return settings.topic();
        }
        String fromHeader = context.headers().get(settings.topicHeaderKey());
        return fromHeader != null && !fromHeader.isBlank() ? fromHeader : settings.topic();
    }

    private String resolveKey(FxContext<String> context) {
        if (settings.keyHeaderKey() != null && !settings.keyHeaderKey().isBlank()) {
            String fromHeader = context.headers().get(settings.keyHeaderKey());
            if (fromHeader != null && !fromHeader.isBlank()) {
                return fromHeader;
            }
        }
        if (context.message().key() != null && !context.message().key().isBlank()) {
            return context.message().key();
        }
        if (context.affinity() != null && !context.affinity().isEmpty()) {
            return context.affinity().value();
        }
        return null;
    }

    private RecordHeaders buildHeaders(FxContext<String> context, String messageKey) {
        RecordHeaders headers = new RecordHeaders();
        headers.add(HEADER_FX_COMMAND, context.command().name().getBytes(StandardCharsets.UTF_8));
        if (context.affinity() != null && !context.affinity().isEmpty()) {
            if (context.affinity().kind() != null) {
                headers.add(HEADER_FX_AFFINITY_KIND, context.affinity().kind().getBytes(StandardCharsets.UTF_8));
            }
            headers.add(HEADER_FX_AFFINITY_VALUE, context.affinity().value().getBytes(StandardCharsets.UTF_8));
        }
        if (messageKey != null) {
            headers.add(HEADER_FX_MESSAGE_KEY, messageKey.getBytes(StandardCharsets.UTF_8));
        }

        if (!settings.includeFxHeaders()) {
            return headers;
        }
        for (Map.Entry<String, String> entry : context.headers().values().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (entry.getValue() == null) {
                continue;
            }
            headers.add("fx-header-" + entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
        }
        return headers;
    }
}

