package com.ees.framework.workflow.dsl;

import com.ees.framework.core.ExecutionMode;
import com.ees.framework.workflow.model.HandlerChainDefinition;
import com.ees.framework.workflow.model.WorkflowDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 선형(리스트 기반) Workflow 정의를 위한 DSL.
 */
public final class WorkflowDsl {
    // 인스턴스를 생성한다.

    private WorkflowDsl() {
    }

    /**
     * 선언형 DSL을 이용해 WorkflowDefinition을 생성한다.
     *
     * @param name 워크플로우 이름
     * @param spec 빌더 설정 람다
     * @return 완성된 WorkflowDefinition
     */
    public static WorkflowDefinition define(String name, Consumer<Builder> spec) {
        Builder builder = new Builder(name);
        spec.accept(builder);
        return builder.build();
    }

    public static final class Builder {

        private final String name;
        private String sourceType;
        private HandlerChainDefinition sourceHandlers;
        private final List<String> pipelineSteps = new ArrayList<>();
        private HandlerChainDefinition sinkHandlers;
        private String sinkType;
        private com.ees.framework.workflow.engine.WorkflowEngine.BatchingOptions batchingOptions;

        /**
         * 워크플로우 빌더를 생성한다.
         *
         * @param name 워크플로우 이름
         */
        public Builder(String name) {
            this.name = name;
        }

        /**
         * 사용할 Source 타입 이름을 설정한다.
         */
        public Builder source(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        /**
         * Source 단계에 적용할 핸들러 체인을 등록한다.
         *
         * @param mode 실행 모드(예: SEQUENTIAL, PARALLEL)
         * @param handlerNames 적용할 핸들러 이름 목록
         */
        public Builder sourceHandlers(ExecutionMode mode, String... handlerNames) {
            this.sourceHandlers = new HandlerChainDefinition(mode, List.of(handlerNames));
            return this;
        }

        /**
         * 순차 실행될 파이프라인 스텝 이름을 추가한다.
         */
        public Builder step(String stepName) {
            this.pipelineSteps.add(stepName);
            return this;
        }

        /**
         * Sink 단계에 적용할 핸들러 체인을 등록한다.
         *
         * @param mode 실행 모드(예: SEQUENTIAL, PARALLEL)
         * @param handlerNames 적용할 핸들러 이름 목록
         */
        public Builder sinkHandlers(ExecutionMode mode, String... handlerNames) {
            this.sinkHandlers = new HandlerChainDefinition(mode, List.of(handlerNames));
            return this;
        }

        /**
         * 사용할 Sink 타입 이름을 설정한다.
         */
        public Builder sink(String sinkType) {
            this.sinkType = sinkType;
            return this;
        }

        /**
         * 워크플로 실행 옵션(배치/백프레셔 등)을 설정한다.
         */
        public Builder batchingOptions(com.ees.framework.workflow.engine.WorkflowEngine.BatchingOptions batchingOptions) {
            this.batchingOptions = batchingOptions;
            return this;
        }

        /**
         * 필수 요소를 검증하고 WorkflowDefinition을 생성한다.
         *
         * @return 완성된 WorkflowDefinition
         * @throws IllegalStateException source/sink 정보가 누락된 경우
         */
        public WorkflowDefinition build() {
            if (sourceType == null) {
                throw new IllegalStateException("sourceType must be set");
            }
            if (sinkType == null) {
                throw new IllegalStateException("sinkType must be set");
            }
            return new WorkflowDefinition(
                name,
                sourceType,
                sourceHandlers,
                pipelineSteps,
                sinkHandlers,
                sinkType,
                batchingOptions
            );
        }
    }
}
