package com.ees.metadatastore;

import java.nio.file.Files;
import java.nio.file.Path;

class FileMetadataStoreContractTest extends AbstractMetadataStoreContractTest {

    @Override
    protected MetadataStore createStore(TestMutableClock clock) throws Exception {
        Path dir = Files.createTempDirectory("file-mds");
        return new FileMetadataStore(dir, new JsonMetadataSerializer(), clock);
    }
}
