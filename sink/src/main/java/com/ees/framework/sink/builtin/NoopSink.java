package com.ees.framework.sink.builtin;

import com.ees.framework.annotations.FxSink;
import com.ees.framework.context.FxContext;
import com.ees.framework.sink.Sink;
import org.springframework.stereotype.Component;
/**
 * 입력을 그대로 무시하고 완료 신호만 반환하는 기본 Sink 구현.
 */
@FxSink("noop")
@Component
public class NoopSink implements Sink<Object> {
    /**
     * write를 수행한다.
     * @param context 
     */

    @Override
    public void write(FxContext<Object> context) {
        // no-op
    }
}
