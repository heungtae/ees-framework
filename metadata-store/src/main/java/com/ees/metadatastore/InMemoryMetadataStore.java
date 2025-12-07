package com.ees.metadatastore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class InMemoryMetadataStore implements MetadataStore {

    private final ConcurrentHashMap<String, StoredValue> store = new ConcurrentHashMap<>();
    private final Sinks.Many<MetadataStoreEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Clock clock;

    public InMemoryMetadataStore() {
        this(Clock.systemUTC());
    }

    public InMemoryMetadataStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public <T> Mono<Boolean> put(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        store.put(key, new StoredValue(value, expiryFor(ttl)));
        publishEvent(key, MetadataStoreEventType.PUT, value);
        return Mono.just(true);
    }

    @Override
    public <T> Mono<Boolean> putIfAbsent(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Instant expiresAt = expiryFor(ttl);
        StoredValue newValue = new StoredValue(value, expiresAt);
        StoredValue existing = store.putIfAbsent(key, newValue);
        if (existing == null) {
            publishEvent(key, MetadataStoreEventType.PUT, value);
            return Mono.just(true);
        }
        if (isExpired(existing)) {
            store.put(key, newValue);
            publishEvent(key, MetadataStoreEventType.PUT, value);
            return Mono.just(true);
        }
        return Mono.just(false);
    }

    @Override
    public <T> Mono<Optional<T>> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        StoredValue value = store.get(key);
        if (value == null || isExpired(value)) {
            store.remove(key);
            return Mono.just(Optional.empty());
        }
        if (!type.isInstance(value.value())) {
            return Mono.just(Optional.empty());
        }
        return Mono.just(Optional.of(type.cast(value.value())));
    }

    @Override
    public Mono<Boolean> delete(String key) {
        Objects.requireNonNull(key, "key must not be null");
        cleanExpired();
        StoredValue removed = store.remove(key);
        if (removed != null) {
            publishEvent(key, MetadataStoreEventType.DELETE, removed.value());
        }
        return Mono.just(removed != null);
    }

    @Override
    public <T> Mono<Boolean> compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(expectedValue, "expectedValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Instant expiresAt = expiryFor(ttl);
        return Mono.fromCallable(() -> {
            StoredValue current = store.get(key);
            if (current == null || isExpired(current) || !Objects.equals(current.value(), expectedValue)) {
                return false;
            }
            store.put(key, new StoredValue(newValue, expiresAt));
            publishEvent(key, MetadataStoreEventType.PUT, newValue);
            return true;
        });
    }

    @Override
    public <T> Flux<T> scan(String prefix, Class<T> type) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        return Flux.fromStream(store.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .filter(value -> !isExpired(value))
                .map(StoredValue::value)
                .filter(type::isInstance)
                .map(type::cast));
    }

    @Override
    public Flux<MetadataStoreEvent> watch(String prefix) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Predicate<MetadataStoreEvent> filter = event -> event.key().startsWith(prefix);
        return sink.asFlux().filter(filter);
    }

    private void cleanExpired() {
        Instant now = clock.instant();
        for (Map.Entry<String, StoredValue> entry : store.entrySet()) {
            if (isExpired(entry.getValue())) {
                store.remove(entry.getKey());
                publishEvent(entry.getKey(), MetadataStoreEventType.EXPIRE, entry.getValue().value(), now);
            }
        }
    }

    private boolean isExpired(StoredValue storedValue) {
        Instant expiresAt = storedValue.expiresAt();
        return expiresAt != null && !expiresAt.isAfter(clock.instant());
    }

    private Instant expiryFor(Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        return clock.instant().plus(ttl);
    }

    private void publishEvent(String key, MetadataStoreEventType type, Object value) {
        publishEvent(key, type, value, clock.instant());
    }

    private void publishEvent(String key, MetadataStoreEventType type, Object value, Instant when) {
        sink.emitNext(new MetadataStoreEvent(key, type, Optional.ofNullable(value), when), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private record StoredValue(Object value, Instant expiresAt) {
    }
}
