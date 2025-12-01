package com.ees.framework.handlers;

import com.ees.framework.context.FxContext;
import reactor.core.publisher.Mono;

/**
 * Sink에 전달되기 직전/직후 컨텍스트에 대해 처리하는 핸들러.
 *
 * @param <T> 처리 대상 레코드 타입
 */
public interface SinkHandler<T> {

    /**
     * 단일 컨텍스트에 대한 처리.
     */
    Mono<FxContext<T>> handle(FxContext<T> context);
}
