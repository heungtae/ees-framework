package com.ees.metadatastore;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for selecting and wiring a MetadataStore backend.
 */
public record MetadataStoreConfig(
    MetadataStoreBackend backend,
    Path filePath,
    String kafkaBootstrapServers,
    String kafkaTopic,
    String kafkaApplicationId,
    Path kafkaStateDir,
    boolean kafkaUseTestDriver
) {

    public MetadataStoreConfig {
        Objects.requireNonNull(backend, "backend must not be null");
    }

    public static MetadataStoreConfig memory() {
        return new MetadataStoreConfig(MetadataStoreBackend.MEMORY, null, null, null, null, null, false);
    }

    public static MetadataStoreConfig file(Path path) {
        return new MetadataStoreConfig(MetadataStoreBackend.FILE, path, null, null, null, null, false);
    }

    public static MetadataStoreConfig kafka(String bootstrapServers, String topic, String applicationId, Path stateDir) {
        return new MetadataStoreConfig(MetadataStoreBackend.KAFKA_KTABLE, null, bootstrapServers, topic, applicationId, stateDir, false);
    }

    public static MetadataStoreConfig kafkaInMemory(String bootstrapServers, String topic, String applicationId, Path stateDir) {
        return new MetadataStoreConfig(MetadataStoreBackend.KAFKA_KTABLE, null, bootstrapServers, topic, applicationId, stateDir, true);
    }
}
