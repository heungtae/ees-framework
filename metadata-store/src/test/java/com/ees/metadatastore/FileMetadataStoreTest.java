package com.ees.metadatastore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FileMetadataStoreTest extends AbstractMetadataStoreContractTest {

    @Override
    protected MetadataStore createStore(TestMutableClock clock) throws Exception {
        Path tempDir = Files.createTempDirectory("metadata-store-test");
        return new FileMetadataStore(tempDir, new JsonMetadataSerializer(), clock);
    }
}
