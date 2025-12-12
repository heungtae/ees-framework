package com.ees.framework.pipeline.builtin;

import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.context.FxContext;
import com.ees.framework.pipeline.PipelineStep;
import org.springframework.stereotype.Component;
/**
 * 입력 컨텍스트를 그대로 통과시키는 기본 스텝.
 */
@FxPipelineStep("pass-through")
@Component
public class PassThroughStep implements PipelineStep<Object, Object> {
    /**
     * apply를 수행한다.
     * @param context 
     * @return 
     */

    @Override
    public FxContext<Object> apply(FxContext<Object> context) {
        return context;
    }
}
