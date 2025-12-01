package com.ees.framework.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * PipelineStep 구현체에 붙이는 어노테이션.
 * value = step 논리 이름.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FxPipelineStep {

    /**
     * Pipeline step 논리 이름 (예: "validateOrder").
     */
    String value();

    /**
     * 기본 순서. 단순 리스트 기반 파이프라인에서 정렬 시 사용 가능.
     */
    int order() default 0;
}
