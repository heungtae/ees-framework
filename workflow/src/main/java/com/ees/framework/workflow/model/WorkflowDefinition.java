package com.ees.framework.workflow.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import com.ees.framework.workflow.engine.WorkflowEngine;

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

    /**
     * 워크플로 실행 옵션(배치/백프레셔 등).
     */
    WorkflowEngine.BatchingOptions batchingOptions;

    /**
     * 모든 구성 요소를 명시적으로 설정하는 생성자.
     *
     * @param name 워크플로우 이름
     * @param sourceType Source 타입 이름
     * @param sourceHandlerChain 소스 핸들러 체인(없으면 null)
     * @param pipelineSteps 순차 실행될 파이프라인 스텝 목록
     * @param sinkHandlerChain 싱크 핸들러 체인(없으면 null)
     * @param sinkType Sink 타입 이름
     * @param batchingOptions 배치/백프레셔 옵션 (null 이면 기본값 사용)
     */
    @Builder
    public WorkflowDefinition(
        String name,
        String sourceType,
        HandlerChainDefinition sourceHandlerChain,
        @Singular List<String> pipelineSteps,
        HandlerChainDefinition sinkHandlerChain,
        String sinkType,
        WorkflowEngine.BatchingOptions batchingOptions
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceHandlerChain = sourceHandlerChain;
        this.pipelineSteps = List.copyOf(pipelineSteps);
        this.sinkHandlerChain = sinkHandlerChain;
        this.sinkType = Objects.requireNonNull(sinkType, "sinkType must not be null");
        this.batchingOptions = batchingOptions == null
            ? WorkflowEngine.BatchingOptions.defaults()
            : batchingOptions;
    }

    /**
     * 기본 배치 옵션을 사용해 워크플로 정의를 생성한다.
     *
     * @param name 워크플로우 이름
     * @param sourceType Source 타입 이름
     * @param sourceHandlerChain 소스 핸들러 체인(없으면 null)
     * @param pipelineSteps 순차 실행될 파이프라인 스텝 목록
     * @param sinkHandlerChain 싱크 핸들러 체인(없으면 null)
     * @param sinkType Sink 타입 이름
     */
    public WorkflowDefinition(
        String name,
        String sourceType,
        HandlerChainDefinition sourceHandlerChain,
        List<String> pipelineSteps,
        HandlerChainDefinition sinkHandlerChain,
        String sinkType
    ) {
        this(name, sourceType, sourceHandlerChain, pipelineSteps, sinkHandlerChain, sinkType,
            WorkflowEngine.BatchingOptions.defaults());
    }
}
