package com.ees.cluster.state;

import com.ees.metadatastore.MetadataStore;
import com.ees.metadatastore.MetadataStoreEvent;
import com.ees.metadatastore.MetadataStoreEventType;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link MetadataStore}를 {@link ClusterStateRepository}로 감싸는 어댑터.
 */
public class MetadataStoreClusterStateRepository implements ClusterStateRepository {

    private final MetadataStore delegate;

    /**
     * 위임할 {@link MetadataStore}를 지정해 생성한다.
     *
     * @param delegate 위임 대상(널 불가)
     */
    public MetadataStoreClusterStateRepository(MetadataStore delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean put(String key, T value, Duration ttl) {
        return delegate.put(key, value, ttl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
        return delegate.putIfAbsent(key, value, ttl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return delegate.get(key, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(String key) {
        return delegate.delete(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
        return delegate.compareAndSet(key, expectedValue, newValue, ttl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> java.util.List<T> scan(String prefix, Class<T> type) {
        return delegate.scan(prefix, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void watch(String prefix, java.util.function.Consumer<ClusterStateEvent> consumer) {
        delegate.watch(prefix, event -> consumer.accept(mapEvent(event)));
    }
    // mapEvent 동작을 수행한다.

    private ClusterStateEvent mapEvent(MetadataStoreEvent event) {
        // metadata-store 이벤트 타입을 cluster-state 이벤트 타입으로 매핑한다.
        ClusterStateEventType mappedType = switch (event.type()) {
            case PUT -> ClusterStateEventType.PUT;
            case DELETE -> ClusterStateEventType.DELETE;
            case EXPIRE -> ClusterStateEventType.EXPIRE;
        };
        return new ClusterStateEvent(event.key(), mappedType, event.value(), event.emittedAt());
    }
}
