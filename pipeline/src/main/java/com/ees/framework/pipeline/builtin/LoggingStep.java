package com.ees.framework.pipeline.builtin;

import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.context.FxContext;
import com.ees.framework.pipeline.PipelineStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 컨텍스트 정보를 로그로 남기고 그대로 반환하는 스텝.
 */
@FxPipelineStep("logging-step")
@Component
public class LoggingStep implements PipelineStep<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(LoggingStep.class);

    @Override
    public Mono<FxContext<Object>> apply(FxContext<Object> context) {
        if (log.isInfoEnabled()) {
            log.info("[LoggingStep] command={} payload={} headers={} meta={}",
                context.command(),
                context.message().payload(),
                context.headers().values(),
                context.meta());
        }
        return Mono.just(context);
    }
}
