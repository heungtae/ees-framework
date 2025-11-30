package com.ees.framework.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Sink 구현체에 붙이는 어노테이션.
 * value = sink 타입 논리 이름 (예: "db-order").
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EesSink {

    /**
     * Sink 타입 논리 이름.
     */
    String value();
}
