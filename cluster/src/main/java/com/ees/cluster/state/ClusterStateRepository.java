package com.ees.cluster.state;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

public interface ClusterStateRepository {

    <T> Mono<Boolean> put(String key, T value, Duration ttl);

    <T> Mono<Boolean> putIfAbsent(String key, T value, Duration ttl);

    <T> Mono<Optional<T>> get(String key, Class<T> type);

    Mono<Boolean> delete(String key);

    <T> Mono<Boolean> compareAndSet(String key, T expectedValue, T newValue, Duration ttl);

    <T> Flux<T> scan(String prefix, Class<T> type);

    Flux<ClusterStateEvent> watch(String prefix);
}
