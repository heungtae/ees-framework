package com.ees.framework.sink.builtin;

import com.ees.framework.annotations.FxSink;
import com.ees.framework.context.FxContext;
import com.ees.framework.sink.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
/**
 * 컨텍스트 내용을 로그로 출력하는 기본 Sink 구현.
 */
@FxSink("logging")
@Component
public class LoggingSink implements Sink<Object> {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(LoggingSink.class);
    /**
     * write를 수행한다.
     * @param context 
     */

    @Override
    public void write(FxContext<Object> context) {
        if (log.isInfoEnabled()) {
            log.info("[LoggingSink] command={} payload={} headers={} meta={}",
                context.command(),
                context.message().payload(),
                context.headers().values(),
                context.meta());
        }
    }
}
