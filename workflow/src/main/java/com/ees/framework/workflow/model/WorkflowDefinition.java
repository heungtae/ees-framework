package com.ees.framework.workflow.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 선형(리스트 기반) Workflow 정의.
 */
public final class WorkflowDefinition {

    private final String name;

    private final String sourceType;
    private final HandlerChainDefinition sourceHandlerChain;

    private final List<String> pipelineSteps;

    private final HandlerChainDefinition sinkHandlerChain;
    private final String sinkType;

    public WorkflowDefinition(
        String name,
        String sourceType,
        HandlerChainDefinition sourceHandlerChain,
        List<String> pipelineSteps,
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

    public String getName() {
        return name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public HandlerChainDefinition getSourceHandlerChain() {
        return sourceHandlerChain;
    }

    public List<String> getPipelineSteps() {
        return Collections.unmodifiableList(pipelineSteps);
    }

    public HandlerChainDefinition getSinkHandlerChain() {
        return sinkHandlerChain;
    }

    public String getSinkType() {
        return sinkType;
    }

    @Override
    public String toString() {
        return "WorkflowDefinition{" +
            "name='" + name + '\'' +
            ", sourceType='" + sourceType + '\'' +
            ", sourceHandlerChain=" + sourceHandlerChain +
            ", pipelineSteps=" + pipelineSteps +
            ", sinkHandlerChain=" + sinkHandlerChain +
            ", sinkType='" + sinkType + '\'' +
            '}';
    }
}
