package com.ees.ai.core;

import com.ees.metadatastore.MetadataStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class MetadataStoreAiSessionServiceTest {

    @Test
    void shouldPersistMessagesWithTtl() {
        RecordingMetadataStore store = new RecordingMetadataStore();
        MetadataStoreAiSessionService service = new MetadataStoreAiSessionService(store, Duration.ofSeconds(60));

        AiMessage first = new AiMessage("user", "hello");
        AiMessage second = new AiMessage("assistant", "hi");

        service.append("sess-1", first).block();
        service.append("sess-1", second).block();

        AiSession session = service.load("sess-1").block();
        Assertions.assertThat(session.messages()).containsExactly(first, second);
        Assertions.assertThat(store.lastTtl).isEqualTo(Duration.ofSeconds(60));
    }

    private static class RecordingMetadataStore implements MetadataStore {

        private final AtomicReference<Object> stored = new AtomicReference<>();
        private Duration lastTtl;

        @Override
        public <T> Mono<Boolean> put(String key, T value, Duration ttl) {
            this.stored.set(value);
            this.lastTtl = ttl;
            return Mono.just(true);
        }

        @Override
        public <T> Mono<Boolean> putIfAbsent(String key, T value, Duration ttl) {
            return put(key, value, ttl);
        }

        @Override
        public <T> Mono<Optional<T>> get(String key, Class<T> type) {
            Object current = stored.get();
            if (current == null || !type.isInstance(current)) {
                return Mono.just(Optional.empty());
            }
            return Mono.just(Optional.of(type.cast(current)));
        }

        @Override
        public Mono<Boolean> delete(String key) {
            return Mono.just(true);
        }

        @Override
        public <T> Mono<Boolean> compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
            return put(key, newValue, ttl);
        }

        @Override
        public <T> Flux<T> scan(String prefix, Class<T> type) {
            return Flux.empty();
        }

        @Override
        public Flux<com.ees.metadatastore.MetadataStoreEvent> watch(String prefix) {
            return Flux.empty();
        }
    }
}
