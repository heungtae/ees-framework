package com.ees.framework.registry;

import com.ees.framework.pipeline.PipelineStep;

/**
 * PipelineStep 레지스트리.
 * 논리적 이름으로 PipelineStep Bean 을 조회한다.
 */
public interface PipelineStepRegistry {

    PipelineStep<?, ?> getByName(String name);
}
