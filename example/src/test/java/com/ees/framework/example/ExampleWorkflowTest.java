package com.ees.framework.example;

import com.ees.framework.core.ExecutionMode;
import com.ees.framework.example.handler.AuditSinkHandler;
import com.ees.framework.example.handler.GreetingSourceHandler;
import com.ees.framework.example.pipeline.UppercasePipelineStep;
import com.ees.framework.example.sink.CollectingSink;
import com.ees.framework.example.source.GreetingSource;
import com.ees.framework.registry.DefaultPipelineStepRegistry;
import com.ees.framework.registry.DefaultSinkHandlerRegistry;
import com.ees.framework.registry.DefaultSinkRegistry;
import com.ees.framework.registry.DefaultSourceHandlerRegistry;
import com.ees.framework.registry.DefaultSourceRegistry;
import com.ees.framework.workflow.DefaultWorkflowNodeResolver;
import com.ees.framework.workflow.engine.ReactorWorkflowEngine;
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
    void runsGreetingWorkflowEndToEnd() {
        GreetingSource source = new GreetingSource(List.of("hello", "team"));
        GreetingSourceHandler sourceHandler = new GreetingSourceHandler();
        UppercasePipelineStep uppercase = new UppercasePipelineStep();
        AuditSinkHandler auditHandler = new AuditSinkHandler();
        CollectingSink sink = new CollectingSink();

        WorkflowDefinition definition = WorkflowDsl.define("example-test-flow", builder -> builder
            .source("example-greeting")
            .sourceHandlers(ExecutionMode.SEQUENTIAL, "greeting-source-handler")
            .step("uppercase-message")
            .sinkHandlers(ExecutionMode.SEQUENTIAL, "audit-sink-handler")
            .sink("example-collector")
        );

        WorkflowRuntime runtime = new WorkflowRuntime(
            List.of(definition),
            List.of(),
            new LinearToGraphConverter(),
            new WorkflowGraphValidator(),
            new ReactorWorkflowEngine(),
            new DefaultWorkflowNodeResolver(
                new DefaultSourceRegistry(List.of(source)),
                new DefaultSourceHandlerRegistry(List.of(sourceHandler)),
                new DefaultPipelineStepRegistry(List.of(uppercase)),
                new DefaultSinkHandlerRegistry(List.of(auditHandler)),
                new DefaultSinkRegistry(List.of(sink))
            )
        );

        runtime.startAll().block();

        assertThat(sink.getReceived()).hasSize(2);
        assertThat(sink.getReceived())
            .extracting(ctx -> ctx.message().payload())
            .containsExactly("HELLO", "TEAM");
        assertThat(sink.getReceived().get(0).headers().get("handled-by"))
            .isEqualTo("GreetingSourceHandler");
        assertThat(sink.getReceived().get(0).meta().attributes())
            .containsKeys("auditedBy", "auditedAt");

        runtime.stopAll().block();
    }
}
