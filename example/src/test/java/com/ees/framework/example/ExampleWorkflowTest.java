package com.ees.framework.example;

import com.ees.framework.core.ExecutionMode;
import com.ees.framework.example.ai.StubAiAgentService;
import com.ees.framework.example.handler.AuditSinkHandler;
import com.ees.framework.example.handler.AiSourceHandler;
import com.ees.framework.example.handler.AlertRoutingSinkHandler;
import com.ees.framework.example.handler.GreetingSourceHandler;
import com.ees.framework.example.pipeline.UppercasePipelineStep;
import com.ees.framework.example.sink.AlertNotificationSink;
import com.ees.framework.example.sink.TriageSink;
import com.ees.framework.example.source.GreetingSource;
import com.ees.framework.registry.DefaultPipelineStepRegistry;
import com.ees.framework.registry.DefaultSinkHandlerRegistry;
import com.ees.framework.registry.DefaultSinkRegistry;
import com.ees.framework.registry.DefaultSourceHandlerRegistry;
import com.ees.framework.registry.DefaultSourceRegistry;
import com.ees.ai.workflow.AiAgentPipelineStep;
import com.ees.framework.workflow.DefaultWorkflowNodeResolver;
import com.ees.framework.workflow.engine.WorkflowEngine;
import com.ees.framework.workflow.engine.WorkflowRuntime;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleWorkflowTest {

    @Test
    void runsGreetingWorkflowEndToEnd() throws Exception {
        GreetingSource source = new GreetingSource(List.of("hello", "team"));
        GreetingSourceHandler sourceHandler = new GreetingSourceHandler();
        AiSourceHandler aiSourceHandler = new AiSourceHandler(
            null,
            "example-greeting"
        );
        UppercasePipelineStep uppercase = new UppercasePipelineStep();
        AiAgentPipelineStep aiStep = new AiAgentPipelineStep(new StubAiAgentService());
        AuditSinkHandler auditHandler = new AuditSinkHandler();
        AlertNotificationSink alertNotificationSink = new AlertNotificationSink();
        AlertRoutingSinkHandler alertRoutingHandler = new AlertRoutingSinkHandler(alertNotificationSink);
        TriageSink sink = new TriageSink();

        WorkflowDefinition definition = WorkflowDsl.define("example-test-flow", builder -> builder
            .source("example-greeting")
            .sourceHandlers(ExecutionMode.SEQUENTIAL, "greeting-source-handler", "ai-source-handler")
            .step("uppercase-message")
            .step("ai-agent-step")
            .sinkHandlers(ExecutionMode.SEQUENTIAL, "audit-sink-handler", "alert-routing-handler")
            .sink("triage-collector")
        );

        WorkflowRuntime runtime = new WorkflowRuntime(
            List.of(definition),
            List.of(),
            new LinearToGraphConverter(),
            new WorkflowGraphValidator(),
            new WorkflowEngine(),
            new DefaultWorkflowNodeResolver(
                new DefaultSourceRegistry(List.of(source)),
                new DefaultSourceHandlerRegistry(List.of(sourceHandler, aiSourceHandler)),
                new DefaultPipelineStepRegistry(List.of(uppercase, aiStep)),
                new DefaultSinkHandlerRegistry(List.of(auditHandler, alertRoutingHandler)),
                new DefaultSinkRegistry(List.of(sink, alertNotificationSink))
            )
        );

        runtime.startAll();

        long deadline = System.currentTimeMillis() + 1_000;
        while (sink.getAlerts().size() + sink.getNormal().size() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertThat(sink.getNormal()).hasSize(2);
        assertThat(sink.getNormal())
            .extracting(ctx -> ctx.message().payload())
            .containsExactly("HELLO", "TEAM");
        assertThat(sink.getNormal().get(0).headers().get("handled-by"))
            .isEqualTo("GreetingSourceHandler");
        assertThat(sink.getNormal().get(0).meta().attributes())
            .containsKeys("auditedBy", "auditedAt", "aiResponse");
        assertThat(alertNotificationSink.getAlerts()).isEmpty();

        runtime.stopAll();
    }
}
