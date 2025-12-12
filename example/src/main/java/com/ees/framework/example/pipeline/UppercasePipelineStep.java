package com.ees.framework.example.pipeline;

import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMessage;
import com.ees.framework.pipeline.PipelineStep;

import java.util.Locale;

/**
 * Transforms the message payload into uppercase while preserving metadata.
 */
@FxPipelineStep("uppercase-message")
public class UppercasePipelineStep implements PipelineStep<String, String> {
    /**
     * apply를 수행한다.
     * @param context 
     * @return 
     */

    @Override
    public FxContext<String> apply(FxContext<String> context) {
        String uppercase = context.message().payload().toUpperCase(Locale.ROOT);
        FxMessage<String> updated = new FxMessage<>(
            context.message().sourceType(),
            uppercase,
            context.message().timestamp(),
            context.message().key()
        );
        return new FxContext<>(context.command(), context.headers(), updated, context.meta(), context.affinity());
    }
}
