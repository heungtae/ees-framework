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

    private WorkflowDsl() {
    }

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

        public Builder(String name) {
            this.name = name;
        }

        public Builder source(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder sourceHandlers(ExecutionMode mode, String... handlerNames) {
            this.sourceHandlers = new HandlerChainDefinition(mode, List.of(handlerNames));
            return this;
        }

        public Builder step(String stepName) {
            this.pipelineSteps.add(stepName);
            return this;
        }

        public Builder sinkHandlers(ExecutionMode mode, String... handlerNames) {
            this.sinkHandlers = new HandlerChainDefinition(mode, List.of(handlerNames));
            return this;
        }

        public Builder sink(String sinkType) {
            this.sinkType = sinkType;
            return this;
        }

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
                sinkType
            );
        }
    }
}
