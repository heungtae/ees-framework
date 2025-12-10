package com.ees.cluster.state;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface ClusterStateRepository {

    <T> boolean put(String key, T value, Duration ttl);

    <T> boolean putIfAbsent(String key, T value, Duration ttl);

    <T> Optional<T> get(String key, Class<T> type);

    boolean delete(String key);

    <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl);

    <T> List<T> scan(String prefix, Class<T> type);

    void watch(String prefix, Consumer<ClusterStateEvent> consumer);
}
