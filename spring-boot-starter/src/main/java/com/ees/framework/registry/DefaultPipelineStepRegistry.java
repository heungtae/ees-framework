package com.ees.framework.registry;

import com.ees.framework.annotations.EesPipelineStep;
import com.ees.framework.pipeline.PipelineStep;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @EesPipelineStep(value=...) 메타데이터를 이용한 기본 PipelineStepRegistry 구현.
 */
public class DefaultPipelineStepRegistry implements PipelineStepRegistry {

    private final Map<String, PipelineStep<?, ?>> byName = new HashMap<>();

    public DefaultPipelineStepRegistry(List<PipelineStep<?, ?>> steps) {
        for (PipelineStep<?, ?> step : steps) {
            EesPipelineStep ann = step.getClass().getAnnotation(EesPipelineStep.class);
            if (ann != null && StringUtils.hasText(ann.value())) {
                byName.put(ann.value(), step);
            }
        }
    }

    @Override
    public PipelineStep<?, ?> getByName(String name) {
        PipelineStep<?, ?> step = byName.get(name);
        if (step == null) {
            throw new IllegalArgumentException("No PipelineStep found for name: " + name);
        }
        return step;
    }
}
