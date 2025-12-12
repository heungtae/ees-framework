package com.ees.metadatastore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryMetadataStoreTest extends AbstractMetadataStoreContractTest {

    @Override
    protected MetadataStore createStore(TestMutableClock clock) {
        return new InMemoryMetadataStore(clock);
    }
}
