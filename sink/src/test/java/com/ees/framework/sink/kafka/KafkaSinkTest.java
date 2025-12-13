package com.ees.framework.sink.kafka;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaSinkTest {

    @Test
    void writeSendsProducerRecordWithResolvedTopicKeyAndHeaders() {
        MockProducer<String, String> producer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());

        KafkaSinkSettings settings = new KafkaSinkSettings(
            "localhost:9092",
            "out",
            "client-1",
            "all",
            false,
            Duration.ofSeconds(1),
            true,
            null,
            null,
            true,
            "kafka-sink-test",
            Map.of()
        );

        KafkaSink sink = new KafkaSink(settings, producer);

        FxHeaders headers = FxHeaders.empty().with("foo", "bar");
        FxMessage<String> message = new FxMessage<>("test", "payload-1", Instant.parse("2025-01-01T00:00:00Z"), "k1");
        FxContext<String> ctx = FxContext.of(message, FxCommand.of("cmd-1"))
            .withHeaders(headers)
            .withAffinity(FxAffinity.of("equipmentId", "eqp-1"));

        sink.write(ctx);

        List<ProducerRecord<String, String>> history = producer.history();
        assertEquals(1, history.size());

        ProducerRecord<String, String> record = history.get(0);
        assertEquals("out", record.topic());
        assertEquals("k1", record.key());
        assertEquals("payload-1", record.value());
        assertNotNull(record.headers().lastHeader(KafkaSink.HEADER_FX_COMMAND));

        assertArrayEquals("cmd-1".getBytes(StandardCharsets.UTF_8),
            record.headers().lastHeader(KafkaSink.HEADER_FX_COMMAND).value());
        assertArrayEquals("bar".getBytes(StandardCharsets.UTF_8),
            record.headers().lastHeader("fx-header-foo").value());
    }
}

