package com.ees.framework.handlers;

import reactor.core.publisher.Mono;

/**
 * Sink에 전달되기 직전/직후 레코드(T)에 대해 처리하는 핸들러.
 *
 * @param <T> 처리 대상 레코드 타입
 */
public interface SinkHandler<T> {

    /**
     * 단일 레코드에 대한 처리.
     */
    Mono<T> handle(T input);
}
