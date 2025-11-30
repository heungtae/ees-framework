package com.ees.framework.handlers;

import reactor.core.publisher.Mono;

/**
 * Source 에서 읽어온 레코드(T)에 대해 전/후처리를 수행하는 핸들러.
 *
 * @param <T> 처리 대상 레코드 타입
 */
public interface SourceHandler<T> {

    /**
     * 단일 레코드에 대한 처리.
     */
    Mono<T> handle(T input);
}
