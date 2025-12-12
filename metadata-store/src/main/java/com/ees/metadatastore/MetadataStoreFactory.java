package com.ees.metadatastore;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Objects;
import java.util.Properties;

/**
 * Factory that selects a MetadataStore implementation based on configuration.
 */
public final class MetadataStoreFactory {
    // 인스턴스를 생성한다.

    private MetadataStoreFactory() {
    }

    /**
     * Create a MetadataStore for the given configuration.
     *
     * @param config backend selection and options
     * @return MetadataStore implementation
     */
    public static MetadataStore create(MetadataStoreConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        return switch (config.backend()) {
            case MEMORY -> new InMemoryMetadataStore();
            case FILE -> new FileMetadataStore(
                Objects.requireNonNull(config.filePath(), "filePath must be set for FILE backend")
            );
            case DB -> throw new UnsupportedOperationException("DbMetadataStore requires DataSource-provided constructor");
            case KAFKA_KTABLE -> {
                Objects.requireNonNull(config.kafkaBootstrapServers(), "kafkaBootstrapServers must be set for Kafka backend");
                Objects.requireNonNull(config.kafkaTopic(), "kafkaTopic must be set for Kafka backend");
                Objects.requireNonNull(config.kafkaApplicationId(), "kafkaApplicationId must be set for Kafka backend");
                Properties props = new Properties();
                props.put(StreamsConfig.APPLICATION_ID_CONFIG, config.kafkaApplicationId());
                props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServers());
                props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
                props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class);
                if (config.kafkaStateDir() != null) {
                    props.put(StreamsConfig.STATE_DIR_CONFIG, config.kafkaStateDir().toString());
                }
                if (config.kafkaUseTestDriver()) {
                    yield KafkaKTableMetadataStore.inMemoryForTests(props, config.kafkaTopic(), new JsonMetadataSerializer(), java.time.Clock.systemUTC());
                }
                yield new KafkaKTableMetadataStore(props, config.kafkaTopic(), new JsonMetadataSerializer(), java.time.Clock.systemUTC());
            }
        };
    }
}
