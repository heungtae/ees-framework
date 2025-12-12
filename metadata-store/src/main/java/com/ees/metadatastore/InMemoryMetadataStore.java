package com.ees.metadatastore;

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

/**
 * 단일 프로세스에서 사용할 수 있는 In-Memory {@link MetadataStore} 구현.
 * <p>
 * TTL 만료 처리는 각 API 호출 시점에 동기적으로 정리(clean-up)하며, prefix 기반 이벤트 watch를 지원한다.
 */
public class InMemoryMetadataStore implements MetadataStore {

    private final ConcurrentHashMap<String, StoredValue> store = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<MetadataStoreEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Clock clock;

    /**
     * 시스템 UTC 시계를 사용해 저장소를 생성한다.
     */
    public InMemoryMetadataStore() {
        this(Clock.systemUTC());
    }

    /**
     * 주어진 시계를 사용해 저장소를 생성한다(테스트 용이성).
     *
     * @param clock 시간 소스(널 불가)
     */
    public InMemoryMetadataStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean put(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        store.put(key, new StoredValue(value, MetadataTtlUtils.expiresAt(clock, ttl)));
        publishEvent(key, MetadataStoreEventType.PUT, value);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Instant expiresAt = MetadataTtlUtils.expiresAt(clock, ttl);
        StoredValue newValue = new StoredValue(value, expiresAt);
        StoredValue existing = store.putIfAbsent(key, newValue);
        if (existing == null) {
            publishEvent(key, MetadataStoreEventType.PUT, value);
            return true;
        }
        if (MetadataTtlUtils.isExpired(clock, existing.expiresAt())) {
            store.put(key, newValue);
            publishEvent(key, MetadataStoreEventType.PUT, value);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        StoredValue value = store.get(key);
        if (value == null || MetadataTtlUtils.isExpired(clock, value.expiresAt())) {
            store.remove(key);
            return Optional.empty();
        }
        if (!type.isInstance(value.value())) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value.value()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(String key) {
        Objects.requireNonNull(key, "key must not be null");
        cleanExpired();
        StoredValue removed = store.remove(key);
        if (removed != null) {
            publishEvent(key, MetadataStoreEventType.DELETE, removed.value());
        }
        return removed != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(expectedValue, "expectedValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Instant expiresAt = MetadataTtlUtils.expiresAt(clock, ttl);
        StoredValue current = store.get(key);
        if (current == null || MetadataTtlUtils.isExpired(clock, current.expiresAt()) || !Objects.equals(current.value(), expectedValue)) {
            return false;
        }
        store.put(key, new StoredValue(newValue, expiresAt));
        publishEvent(key, MetadataStoreEventType.PUT, newValue);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> java.util.List<T> scan(String prefix, Class<T> type) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        return store.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .map(Map.Entry::getValue)
            .filter(value -> !MetadataTtlUtils.isExpired(clock, value.expiresAt()))
            .map(StoredValue::value)
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void watch(String prefix, Consumer<MetadataStoreEvent> consumer) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        Predicate<MetadataStoreEvent> filter = event -> event.key().startsWith(prefix);
        listeners.add(event -> {
            if (filter.test(event)) {
                consumer.accept(event);
            }
        });
    }
    // cleanExpired 동작을 수행한다.

    private void cleanExpired() {
        // store 전반을 순회하며 TTL 만료 데이터를 제거하고 EXPIRE 이벤트를 발행한다.
        Instant now = clock.instant();
        for (Map.Entry<String, StoredValue> entry : store.entrySet()) {
            if (MetadataTtlUtils.isExpired(clock, entry.getValue().expiresAt())) {
                store.remove(entry.getKey());
                publishEvent(entry.getKey(), MetadataStoreEventType.EXPIRE, entry.getValue().value(), now);
            }
        }
    }
    // publishEvent 동작을 수행한다.

    private void publishEvent(String key, MetadataStoreEventType type, Object value) {
        // 이벤트 발행 시각을 현재 시각으로 설정한다.
        publishEvent(key, type, value, clock.instant());
    }
    // publishEvent 동작을 수행한다.

    private void publishEvent(String key, MetadataStoreEventType type, Object value, Instant when) {
        // prefix 필터링은 watch 등록 시 래핑된 listener에서 수행한다.
        MetadataStoreEvent event = new MetadataStoreEvent(key, type, Optional.ofNullable(value), when);
        for (Consumer<MetadataStoreEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private record StoredValue(Object value, Instant expiresAt) {
    }
}
