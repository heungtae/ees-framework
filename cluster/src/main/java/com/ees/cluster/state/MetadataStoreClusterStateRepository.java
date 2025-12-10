package com.ees.cluster.state;

import com.ees.metadatastore.MetadataStore;
import com.ees.metadatastore.MetadataStoreEvent;
import com.ees.metadatastore.MetadataStoreEventType;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MetadataStoreClusterStateRepository implements ClusterStateRepository {

    private final MetadataStore delegate;

    public MetadataStoreClusterStateRepository(MetadataStore delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public <T> boolean put(String key, T value, Duration ttl) {
        return delegate.put(key, value, ttl);
    }

    @Override
    public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
        return delegate.putIfAbsent(key, value, ttl);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return delegate.get(key, type);
    }

    @Override
    public boolean delete(String key) {
        return delegate.delete(key);
    }

    @Override
    public <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
        return delegate.compareAndSet(key, expectedValue, newValue, ttl);
    }

    @Override
    public <T> java.util.List<T> scan(String prefix, Class<T> type) {
        return delegate.scan(prefix, type);
    }

    @Override
    public void watch(String prefix, java.util.function.Consumer<ClusterStateEvent> consumer) {
        delegate.watch(prefix, event -> consumer.accept(mapEvent(event)));
    }

    private ClusterStateEvent mapEvent(MetadataStoreEvent event) {
        ClusterStateEventType mappedType = switch (event.type()) {
            case PUT -> ClusterStateEventType.PUT;
            case DELETE -> ClusterStateEventType.DELETE;
            case EXPIRE -> ClusterStateEventType.EXPIRE;
        };
        return new ClusterStateEvent(event.key(), mappedType, event.value(), event.emittedAt());
    }
}
