package com.ees.framework.workflow.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 선형(리스트 기반) Workflow 정의.
 */
public final class WorkflowDefinition {

    private final String name;

    private final String sourceType; // 사용될 Source 타입 이름
    private final HandlerChainDefinition sourceHandlerChain; // 소스에 적용할 핸들러 체인

    private final List<String> pipelineSteps; // 순차 실행될 파이프라인 스텝 이름들

    private final HandlerChainDefinition sinkHandlerChain; // 싱크 전달 전후 핸들러 체인
    private final String sinkType; // 사용될 Sink 타입 이름

    /**
     * 선형 워크플로우 정의를 생성한다.
     *
     * @param name               워크플로우 이름
     * @param sourceType         Source 타입 이름
     * @param sourceHandlerChain Source 핸들러 체인 정의 (nullable)
     * @param pipelineSteps      순차 실행될 파이프라인 스텝 이름 목록
     * @param sinkHandlerChain   Sink 핸들러 체인 정의 (nullable)
     * @param sinkType           Sink 타입 이름
     */
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

    /** 워크플로우 이름 */
    public String getName() {
        return name;
    }

    /** Source 타입 이름 */
    public String getSourceType() {
        return sourceType;
    }

    /** Source 핸들러 체인 정의 (없을 수 있음) */
    public HandlerChainDefinition getSourceHandlerChain() {
        return sourceHandlerChain;
    }

    /** 순차 실행될 파이프라인 스텝 이름 목록 */
    public List<String> getPipelineSteps() {
        return Collections.unmodifiableList(pipelineSteps);
    }

    /** Sink 핸들러 체인 정의 (없을 수 있음) */
    public HandlerChainDefinition getSinkHandlerChain() {
        return sinkHandlerChain;
    }

    /** Sink 타입 이름 */
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
