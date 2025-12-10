package com.ees.cluster.state;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InMemoryClusterStateRepository implements ClusterStateRepository {

    private final ConcurrentHashMap<String, StoredValue> store = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<ClusterStateEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public InMemoryClusterStateRepository() {
        this(Clock.systemUTC());
    }

    public InMemoryClusterStateRepository(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public <T> boolean put(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        store.put(key, new StoredValue(value, expiryFor(ttl)));
        publishEvent(key, ClusterStateEventType.PUT, value);
        return true;
    }

    @Override
    public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Instant expiresAt = expiryFor(ttl);
        StoredValue newValue = new StoredValue(value, expiresAt);
        StoredValue existing = store.putIfAbsent(key, newValue);
        if (existing == null) {
            publishEvent(key, ClusterStateEventType.PUT, value);
            return true;
        }
        if (isExpired(existing)) {
            store.put(key, newValue);
            publishEvent(key, ClusterStateEventType.PUT, value);
            return true;
        }
        return false;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        StoredValue value = store.get(key);
        if (value == null || isExpired(value)) {
            store.remove(key);
            return Optional.empty();
        }
        if (!type.isInstance(value.value())) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value.value()));
    }

    @Override
    public boolean delete(String key) {
        Objects.requireNonNull(key, "key must not be null");
        cleanExpired();
        StoredValue removed = store.remove(key);
        if (removed != null) {
            publishEvent(key, ClusterStateEventType.DELETE, removed.value());
        }
        return removed != null;
    }

    @Override
    public <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(expectedValue, "expectedValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Instant expiresAt = expiryFor(ttl);
        StoredValue current = store.get(key);
        if (current == null || isExpired(current) || !Objects.equals(current.value(), expectedValue)) {
            return false;
        }
        store.put(key, new StoredValue(newValue, expiresAt));
        publishEvent(key, ClusterStateEventType.PUT, newValue);
        return true;
    }

    @Override
    public <T> java.util.List<T> scan(String prefix, Class<T> type) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        return store.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .map(Map.Entry::getValue)
            .filter(value -> !isExpired(value))
            .map(StoredValue::value)
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    @Override
    public void watch(String prefix, Consumer<ClusterStateEvent> consumer) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        Predicate<ClusterStateEvent> filter = event -> event.key().startsWith(prefix);
        listeners.add(event -> {
            if (filter.test(event)) {
                consumer.accept(event);
            }
        });
    }

    private void cleanExpired() {
        Instant now = clock.instant();
        for (Map.Entry<String, StoredValue> entry : store.entrySet()) {
            if (isExpired(entry.getValue())) {
                store.remove(entry.getKey());
                publishEvent(entry.getKey(), ClusterStateEventType.EXPIRE, entry.getValue().value(), now);
            }
        }
    }

    private boolean isExpired(StoredValue storedValue) {
        Instant expiresAt = storedValue.expiresAt();
        return expiresAt != null && (expiresAt.isBefore(clock.instant()) || expiresAt.equals(clock.instant()));
    }

    private Instant expiryFor(Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        return clock.instant().plus(ttl);
    }

    private void publishEvent(String key, ClusterStateEventType type, Object value) {
        publishEvent(key, type, value, clock.instant());
    }

    private void publishEvent(String key, ClusterStateEventType type, Object value, Instant when) {
        ClusterStateEvent event = new ClusterStateEvent(key, type, Optional.ofNullable(value), when);
        for (Consumer<ClusterStateEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private record StoredValue(Object value, Instant expiresAt) {
    }
}
