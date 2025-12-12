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

/**
 * 단일 프로세스에서 사용할 수 있는 in-memory {@link ClusterStateRepository} 구현.
 * <p>
 * TTL 정리는 각 API 호출 시점에 동기적으로 수행한다.
 */
public class InMemoryClusterStateRepository implements ClusterStateRepository {

    private final ConcurrentHashMap<String, StoredValue> store = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<ClusterStateEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Clock clock;

    /**
     * 시스템 UTC 시계를 사용해 저장소를 생성한다.
     */
    public InMemoryClusterStateRepository() {
        this(Clock.systemUTC());
    }

    /**
     * 주어진 시계를 사용해 저장소를 생성한다(테스트 용이성).
     *
     * @param clock 시간 소스(널 불가)
     */
    public InMemoryClusterStateRepository(Clock clock) {
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
        store.put(key, new StoredValue(value, expiryFor(ttl)));
        publishEvent(key, ClusterStateEventType.PUT, value);
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
        Instant expiresAt = expiryFor(ttl);
        StoredValue current = store.get(key);
        if (current == null || isExpired(current) || !Objects.equals(current.value(), expectedValue)) {
            return false;
        }
        store.put(key, new StoredValue(newValue, expiresAt));
        publishEvent(key, ClusterStateEventType.PUT, newValue);
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
            .filter(value -> !isExpired(value))
            .map(StoredValue::value)
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
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
    // cleanExpired 동작을 수행한다.

    private void cleanExpired() {
        // TTL 만료 데이터를 제거하고 EXPIRE 이벤트를 발행한다.
        Instant now = clock.instant();
        for (Map.Entry<String, StoredValue> entry : store.entrySet()) {
            if (isExpired(entry.getValue())) {
                store.remove(entry.getKey());
                publishEvent(entry.getKey(), ClusterStateEventType.EXPIRE, entry.getValue().value(), now);
            }
        }
    }
    // expired 여부를 반환한다.

    private boolean isExpired(StoredValue storedValue) {
        // 만료 시각이 존재할 때만 만료 여부를 판단한다(null은 무제한 TTL로 취급).
        Instant expiresAt = storedValue.expiresAt();
        return expiresAt != null && (expiresAt.isBefore(clock.instant()) || expiresAt.equals(clock.instant()));
    }
    // expiryFor 동작을 수행한다.

    private Instant expiryFor(Duration ttl) {
        // 0 이하 TTL은 "만료 없음"으로 처리한다.
        if (ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        return clock.instant().plus(ttl);
    }
    // publishEvent 동작을 수행한다.

    private void publishEvent(String key, ClusterStateEventType type, Object value) {
        // 이벤트 발생 시각을 현재 시각으로 설정한다.
        publishEvent(key, type, value, clock.instant());
    }
    // publishEvent 동작을 수행한다.

    private void publishEvent(String key, ClusterStateEventType type, Object value, Instant when) {
        // prefix 필터링은 watch 등록 시 래핑된 listener에서 수행한다.
        ClusterStateEvent event = new ClusterStateEvent(key, type, Optional.ofNullable(value), when);
        for (Consumer<ClusterStateEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private record StoredValue(Object value, Instant expiresAt) {
    }
}
