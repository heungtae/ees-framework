package com.ees.framework.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Source 구현체에 붙이는 어노테이션.
 * type 값은 논리적 Source 타입 이름으로 사용된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FxSource {

    /**
     * Source 타입 논리 이름 (예: "kafka-order").
     */
    String type();
}
