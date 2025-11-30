package com.ees.framework.sink;

import reactor.core.publisher.Mono;

/**
 * 출력(Sink) 추상화.
 *
 * @param <T> 저장하거나 전송할 레코드 타입
 */
public interface Sink<T> {

    /**
     * 레코드 스트림을 받아서 저장/전송을 수행.
     */
    Mono<Void> write(T record);
}
