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

class InMemoryMetadataStoreTest {

    private TestClock clock;
    private InMemoryMetadataStore store;

    @BeforeEach
    void setUp() {
        clock = new TestClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        store = new InMemoryMetadataStore(clock);
    }

    @Test
    void putAndExpire() {
        store.put("key", "value", Duration.ofSeconds(1));
        Optional<String> value = store.get("key", String.class);
        assertTrue(value.isPresent());
        assertEquals("value", value.get());

        clock.advance(Duration.ofSeconds(2));
        Optional<String> expired = store.get("key", String.class);
        assertTrue(expired.isEmpty());
    }

    @Test
    void watchEmitsEvents() {
        List<MetadataStoreEvent> events = new ArrayList<>();
        store.watch("prefix", events::add);

        store.put("prefix/a", "v", Duration.ofSeconds(5));

        assertEquals(1, events.size());
        assertEquals(MetadataStoreEventType.PUT, events.get(0).type());
    }

    private static class TestClock extends Clock {
        private Instant now;
        private final ZoneId zoneId;

        TestClock(Instant seed, ZoneId zoneId) {
            this.now = seed;
            this.zoneId = zoneId;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new TestClock(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
