package com.ees.framework.pipeline.builtin;

import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.pipeline.PipelineStep;
import org.springframework.stereotype.Component;
import java.time.Instant;

/**
 * 간단한 헤더 정보를 추가하는 기본 스텝.
 */
@FxPipelineStep("enrich-header")
@Component
public class HeaderEnrichmentStep implements PipelineStep<Object, Object> {
    /**
     * apply를 수행한다.
     * @param context 
     * @return 
     */

    @Override
    public FxContext<Object> apply(FxContext<Object> context) {
        FxHeaders headers = context.headers()
            .with("processed-by", getClass().getSimpleName())
            .with("processed-at", Instant.now().toString());
        return context.withHeaders(headers);
    }
}
