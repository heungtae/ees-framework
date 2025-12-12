package com.ees.framework.example;

import com.ees.ai.core.AiAgentService;
import com.ees.framework.example.ai.AiResultSink;
import com.ees.framework.example.ai.AiWorkflowSource;
import com.ees.framework.example.ai.StubAiAgentService;
import com.ees.framework.registry.DefaultPipelineStepRegistry;
import com.ees.framework.registry.DefaultSinkHandlerRegistry;
import com.ees.framework.registry.DefaultSinkRegistry;
import com.ees.framework.registry.DefaultSourceHandlerRegistry;
import com.ees.framework.registry.DefaultSourceRegistry;
import com.ees.framework.workflow.DefaultWorkflowNodeResolver;
import com.ees.framework.workflow.engine.WorkflowEngine;
import com.ees.framework.workflow.engine.WorkflowRuntime;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import com.ees.ai.workflow.AiAgentPipelineStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleAiWorkflowTest {

    @Test
    void runsAiWorkflowEndToEnd() {
        AiWorkflowSource source = new AiWorkflowSource(
            List.of("장애 리포트를 요약해", "릴리스 노트를 요약해"),
            "너는 운영 리포트를 요약하고 액션을 제시하는 분석가다."
        );
        AiAgentService stubAiService = new StubAiAgentService();
        AiAgentPipelineStep aiStep = new AiAgentPipelineStep(stubAiService);
        AiResultSink sink = new AiResultSink();

        WorkflowDefinition definition = WorkflowDsl.define("example-ai-workflow", builder -> builder
            .source("ai-demo-source")
            .step("ai-agent-step")
            .sink("ai-demo-collector")
        );

        WorkflowRuntime runtime = new WorkflowRuntime(
            List.of(definition),
            List.of(),
            new LinearToGraphConverter(),
            new WorkflowGraphValidator(),
            new WorkflowEngine(),
            new DefaultWorkflowNodeResolver(
                new DefaultSourceRegistry(List.of(source)),
                new DefaultSourceHandlerRegistry(List.of()),
                new DefaultPipelineStepRegistry(List.of(aiStep)),
                new DefaultSinkHandlerRegistry(List.of()),
                new DefaultSinkRegistry(List.of(sink))
            )
        );

        runtime.startAll();

        assertThat(sink.getReceived()).hasSize(2);
        assertThat(sink.getReceived())
            .allSatisfy(ctx -> {
                assertThat(ctx.meta().attributes()).containsKey("aiResponse");
                String aiResponse = ctx.meta().attributes().get("aiResponse").toString();
                assertThat(aiResponse).contains("SUMMARY").contains("ACTION");
            });

        runtime.stopAll();
    }
}
