package com.ees.framework.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * SourceHandler 구현체에 붙이는 어노테이션.
 * value = handler 논리 이름.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface SourceHandlerComponent {

    /**
     * Handler 논리 이름.
     */
    String value();

    /**
     * 기본 순서. 필요시 정렬에 사용 가능.
     */
    int order() default 0;
}
