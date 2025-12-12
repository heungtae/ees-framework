package com.ees.framework.example.ai;

import com.ees.framework.annotations.FxSink;
import com.ees.framework.context.FxContext;
import com.ees.framework.sink.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI 워크플로 결과를 수집하는 싱크.
 */
@FxSink("ai-demo-collector")
@Component
public class AiResultSink implements Sink<Object> {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(AiResultSink.class);
    private final List<FxContext<Object>> received = new CopyOnWriteArrayList<>();
    /**
     * write를 수행한다.
     * @param context 
     */

    @Override
    public void write(FxContext<Object> context) {
        received.add(context);
        log.info("AI result collected payload={} aiResponse={}", context.message().payload(),
            context.meta().attributes().get("aiResponse"));
    }
    /**
     * received를 반환한다.
     * @return 
     */

    public List<FxContext<Object>> getReceived() {
        return List.copyOf(received);
    }
}
