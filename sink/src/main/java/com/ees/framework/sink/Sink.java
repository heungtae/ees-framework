package com.ees.framework.sink;

import com.ees.framework.context.FxContext;
import com.ees.framework.context.SupportsContext;
/**
 * 출력(Sink) 추상화.
 *
 * @param <T> 저장하거나 전송할 레코드 타입
 */
public interface Sink<T> extends SupportsContext {

    /**
     * 컨텍스트를 받아서 저장/전송을 수행.
     */
    void write(FxContext<T> context);
}
