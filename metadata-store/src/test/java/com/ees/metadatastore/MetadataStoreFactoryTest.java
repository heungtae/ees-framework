package com.ees.metadatastore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetadataStoreFactoryTest {

    @Test
    void createsInMemoryStoreByDefault() {
        MetadataStore store = MetadataStoreFactory.create(MetadataStoreConfig.memory());
        assertThat(store).isInstanceOf(InMemoryMetadataStore.class);
    }

    @Test
    void throwsForUnsupportedBackends() {
        assertThrows(UnsupportedOperationException.class, () ->
            MetadataStoreFactory.create(new MetadataStoreConfig(MetadataStoreBackend.DB, null, null, null, null, null, false))
        );
    }

    @Test
    void createsFileStoreWhenPathProvided() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("mds-factory");
        MetadataStore store = MetadataStoreFactory.create(MetadataStoreConfig.file(dir));
        assertThat(store).isInstanceOf(FileMetadataStore.class);
    }

    @Test
    void createsKafkaStoreWithTestDriver() {
        MetadataStoreConfig config = MetadataStoreConfig.kafkaInMemory("dummy:9092", "mds-topic", "mds-app", null);
        MetadataStore store = MetadataStoreFactory.create(config);
        assertThat(store).isInstanceOf(KafkaKTableMetadataStore.class);
    }
}
