package com.ees.framework.pipeline;

import com.ees.framework.context.FxContext;
import reactor.core.publisher.Mono;

/**
 * Pipeline 내부에서 사용되는 변환 단계.
 *
 * @param <I> 입력 타입
 * @param <O> 출력 타입
 */
public interface PipelineStep<I, O> {

    /**
     * 단일 컨텍스트를 입력받아 변환된 컨텍스트를 반환.
     */
    Mono<FxContext<O>> apply(FxContext<I> context);
}
