package com.ees.metadatastore;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaKTableMetadataStoreTest extends AbstractMetadataStoreContractTest {

    @Override
    protected MetadataStore createStore(TestMutableClock clock) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "mds-test-" + UUID.randomUUID());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.STATE_DIR_CONFIG, Files.createTempDirectory("kafka-md-state").toString());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class);
        return KafkaKTableMetadataStore.inMemoryForTests(props, "metadata-store", new JsonMetadataSerializer(), clock);
    }

    @Test
    void expireEventEmittedOnTtl() throws Exception {
        TestMutableClock clock = TestMutableClock.fixedNow();
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "mds-expire-" + UUID.randomUUID());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.STATE_DIR_CONFIG, Files.createTempDirectory("kafka-md-expire").toString());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class);
        KafkaKTableMetadataStore store = KafkaKTableMetadataStore.inMemoryForTests(props, "metadata-store-expire", new JsonMetadataSerializer(), clock);
        List<MetadataStoreEvent> events = new ArrayList<>();
        store.watch("k/", events::add);

        store.put("k/1", "v1", Duration.ofSeconds(1));
        clock.advance(Duration.ofSeconds(2));
        store.get("k/1", String.class);

        assertThat(events).extracting(MetadataStoreEvent::type)
            .contains(MetadataStoreEventType.PUT, MetadataStoreEventType.EXPIRE);
    }
}
