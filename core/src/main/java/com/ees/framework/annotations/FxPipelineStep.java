package com.ees.framework.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * {@link com.ees.framework.pipeline.PipelineStep} 구현체에 붙이는 어노테이션.
 * <p>
 * {@link com.ees.framework.registry.PipelineStepRegistry}가 이 메타데이터를 이용해
 * "step name 문자열" → "PipelineStep 구현체"를 매핑한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FxPipelineStep {

    /**
     * Pipeline step 논리 이름.
     * <p>
     * 동일 애플리케이션 컨텍스트 내에서 값은 유일해야 한다.
     *
     * @return step 이름(예: {@code "validateOrder"})
     */
    String value();

    /**
     * 기본 순서.
     * <p>
     * 단순 리스트 기반 파이프라인에서 정렬 시 사용할 수 있다.
     *
     * @return order 값(작을수록 먼저)
     */
    int order() default 0;
}
