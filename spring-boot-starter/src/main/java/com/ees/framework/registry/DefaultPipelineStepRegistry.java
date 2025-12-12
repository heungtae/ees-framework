package com.ees.framework.registry;

import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.pipeline.PipelineStep;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @FxPipelineStep(value=...) 메타데이터를 이용한 기본 PipelineStepRegistry 구현.
 */
public class DefaultPipelineStepRegistry implements PipelineStepRegistry {

    private final Map<String, PipelineStep<?, ?>> byName = new HashMap<>();
    /**
     * 인스턴스를 생성한다.
     * @param steps 
     */

    public DefaultPipelineStepRegistry(List<PipelineStep<?, ?>> steps) {
        for (PipelineStep<?, ?> step : steps) {
            FxPipelineStep ann = step.getClass().getAnnotation(FxPipelineStep.class);
            if (ann != null && StringUtils.hasText(ann.value())) {
                byName.put(ann.value(), step);
            }
        }
    }
    /**
     * byName를 반환한다.
     * @param name 
     * @return 
     */

    @Override
    public PipelineStep<?, ?> getByName(String name) {
        PipelineStep<?, ?> step = byName.get(name);
        if (step == null) {
            throw new IllegalArgumentException("No PipelineStep found for name: " + name);
        }
        return step;
    }
}
