package com.ees.framework.messaging;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 메시징 인프라 추상화 인터페이스.
 */
public interface MessageBus {

    Mono<Void> publish(String topic, byte[] payload);

    Flux<byte[]> subscribe(String topic);
}
