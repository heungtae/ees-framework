package com.ees.framework.pipeline;

import reactor.core.publisher.Flux;

/**
 * Pipeline 내부에서 사용되는 변환 단계.
 * Flux<I>를 받아 Flux<O>로 변환하는 역할.
 *
 * @param <I> 입력 타입
 * @param <O> 출력 타입
 */
public interface PipelineStep<I, O> {

    /**
     * 입력 스트림을 받아 변환된 출력 스트림을 반환.
     */
    Flux<O> apply(Flux<I> input);
}
