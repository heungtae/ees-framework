package com.ees.framework.workflow.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * 선형(리스트 기반) Workflow 정의.
 */
@Value
public class WorkflowDefinition {

    /**
     * 워크플로우 이름.
     */
    String name;

    /**
     * 사용될 Source 타입 이름.
     */
    String sourceType;

    /**
     * 소스에 적용할 핸들러 체인.
     */
    HandlerChainDefinition sourceHandlerChain;

    /**
     * 순차 실행될 파이프라인 스텝 이름들.
     */
    List<String> pipelineSteps;

    /**
     * 싱크 전달 전후 핸들러 체인.
     */
    HandlerChainDefinition sinkHandlerChain;

    /**
     * 사용될 Sink 타입 이름.
     */
    String sinkType;

    @Builder
    public WorkflowDefinition(
        String name,
        String sourceType,
        HandlerChainDefinition sourceHandlerChain,
        @Singular List<String> pipelineSteps,
        HandlerChainDefinition sinkHandlerChain,
        String sinkType
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceHandlerChain = sourceHandlerChain;
        this.pipelineSteps = List.copyOf(pipelineSteps);
        this.sinkHandlerChain = sinkHandlerChain;
        this.sinkType = Objects.requireNonNull(sinkType, "sinkType must not be null");
    }
}
