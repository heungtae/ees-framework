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
    /**
     * memory를 수행한다.
     * @return 
     */

    public static MetadataStoreConfig memory() {
        return new MetadataStoreConfig(MetadataStoreBackend.MEMORY, null, null, null, null, null, false);
    }
    /**
     * file를 수행한다.
     * @param path 
     * @return 
     */

    public static MetadataStoreConfig file(Path path) {
        return new MetadataStoreConfig(MetadataStoreBackend.FILE, path, null, null, null, null, false);
    }
    /**
     * kafka를 수행한다.
     * @param bootstrapServers 
     * @param topic 
     * @param applicationId 
     * @param stateDir 
     * @return 
     */

    public static MetadataStoreConfig kafka(String bootstrapServers, String topic, String applicationId, Path stateDir) {
        return new MetadataStoreConfig(MetadataStoreBackend.KAFKA_KTABLE, null, bootstrapServers, topic, applicationId, stateDir, false);
    }
    /**
     * kafkaInMemory를 수행한다.
     * @param bootstrapServers 
     * @param topic 
     * @param applicationId 
     * @param stateDir 
     * @return 
     */

    public static MetadataStoreConfig kafkaInMemory(String bootstrapServers, String topic, String applicationId, Path stateDir) {
        return new MetadataStoreConfig(MetadataStoreBackend.KAFKA_KTABLE, null, bootstrapServers, topic, applicationId, stateDir, true);
    }
}
