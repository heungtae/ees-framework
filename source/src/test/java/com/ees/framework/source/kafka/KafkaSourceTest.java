package com.ees.framework.source.kafka;

import com.ees.framework.context.FxContext;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaSourceTest {

    @Test
    void readMapsConsumerRecordToFxContext() {
        MockConsumer<String, String> consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("orders", 0);
        consumer.assign(List.of(tp));
        consumer.updateBeginningOffsets(Map.of(tp, 0L));
        consumer.addRecord(new ConsumerRecord<>("orders", 0, 12L, "eqp-1", "payload-1"));

        KafkaSourceSettings settings = new KafkaSourceSettings(
            "localhost:9092",
            List.of("orders"),
            "ees",
            null,
            "ingest",
            "equipmentId",
            Duration.ofMillis(10),
            10,
            true,
            "earliest",
            "kafka-source-test",
            Map.of()
        );

        KafkaSource source = new KafkaSource(settings, consumer);

        Iterable<FxContext<String>> batch = source.read();
        FxContext<String> ctx = batch.iterator().next();

        assertEquals("payload-1", ctx.message().payload());
        assertEquals("eqp-1", ctx.message().key());
        assertEquals("equipmentId", ctx.affinity().kind());
        assertEquals("eqp-1", ctx.affinity().value());

        assertEquals("equipmentId", ctx.headers().get(KafkaSource.HEADER_AFFINITY_KIND));
        assertEquals("eqp-1", ctx.headers().get(KafkaSource.HEADER_AFFINITY_VALUE));
        assertEquals("orders", ctx.headers().get(KafkaSource.HEADER_KAFKA_TOPIC));
        assertEquals("0", ctx.headers().get(KafkaSource.HEADER_KAFKA_PARTITION));
        assertEquals("12", ctx.headers().get(KafkaSource.HEADER_KAFKA_OFFSET));

        assertNotNull(ctx.meta());
        assertEquals("kafka-source-test", ctx.meta().sourceId());
        assertTrue(ctx.meta().attributes().containsKey("kafka.offset"));
    }
}

