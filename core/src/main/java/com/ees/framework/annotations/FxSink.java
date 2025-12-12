package com.ees.framework.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * {@link com.ees.framework.sink.Sink} 구현체에 붙이는 어노테이션.
 * <p>
 * {@link com.ees.framework.registry.SinkRegistry}가 이 메타데이터를 이용해
 * "sink type 문자열" → "Sink 구현체"를 매핑한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FxSink {

    /**
     * Sink 타입 논리 이름.
     * <p>
     * 동일 애플리케이션 컨텍스트 내에서 값은 유일해야 한다.
     *
     * @return Sink 타입 문자열(예: {@code "db-order"})
     */
    String value();
}
