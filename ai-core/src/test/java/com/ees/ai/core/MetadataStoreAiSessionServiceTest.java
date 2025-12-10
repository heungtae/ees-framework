package com.ees.ai.core;

import com.ees.metadatastore.MetadataStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataStoreAiSessionServiceTest {

    @Test
    void shouldPersistMessagesWithTtl() {
        RecordingMetadataStore store = new RecordingMetadataStore();
        MetadataStoreAiSessionService service = new MetadataStoreAiSessionService(store, Duration.ofSeconds(60));

        AiMessage first = new AiMessage("user", "hello");
        AiMessage second = new AiMessage("assistant", "hi");

        service.append("sess-1", first);
        service.append("sess-1", second);

        AiSession session = service.load("sess-1");
        Assertions.assertThat(session.messages()).containsExactly(first, second);
        Assertions.assertThat(store.lastTtl).isEqualTo(Duration.ofSeconds(60));
    }

    private static class RecordingMetadataStore implements MetadataStore {

        private final AtomicReference<Object> stored = new AtomicReference<>();
        private Duration lastTtl;

        @Override
        public <T> boolean put(String key, T value, Duration ttl) {
            this.stored.set(value);
            this.lastTtl = ttl;
            return true;
        }

        @Override
        public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
            return put(key, value, ttl);
        }

        @Override
        public <T> Optional<T> get(String key, Class<T> type) {
            Object current = stored.get();
            if (current == null || !type.isInstance(current)) {
                return Optional.empty();
            }
            return Optional.of(type.cast(current));
        }

        @Override
        public boolean delete(String key) {
            return true;
        }

        @Override
        public <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
            return put(key, newValue, ttl);
        }

        @Override
        public <T> java.util.List<T> scan(String prefix, Class<T> type) {
            return java.util.List.of();
        }

        @Override
        public void watch(String prefix, java.util.function.Consumer<com.ees.metadatastore.MetadataStoreEvent> consumer) {
        }
    }
}
