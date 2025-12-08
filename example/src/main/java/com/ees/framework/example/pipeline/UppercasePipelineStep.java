package com.ees.framework.example.pipeline;

import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMessage;
import com.ees.framework.pipeline.PipelineStep;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Transforms the message payload into uppercase while preserving metadata.
 */
@FxPipelineStep("uppercase-message")
public class UppercasePipelineStep implements PipelineStep<String, String> {

    @Override
    public Mono<FxContext<String>> apply(FxContext<String> context) {
        String uppercase = context.message().payload().toUpperCase(Locale.ROOT);
        FxMessage<String> updated = new FxMessage<>(
            context.message().sourceType(),
            uppercase,
            context.message().timestamp(),
            context.message().key()
        );
        return Mono.just(new FxContext<>(context.command(), context.headers(), updated, context.meta()));
    }
}
