package com.ees.framework.example.config;

import com.ees.framework.core.ExecutionMode;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the custom components into a runnable workflow definition.
 */
@Configuration
public class ExampleWorkflowConfiguration {

    @Bean
    public WorkflowDefinition exampleGreetingWorkflow() {
        return WorkflowDsl.define("example-greeting-workflow", builder -> builder
            .source("example-greeting")
            .sourceHandlers(ExecutionMode.SEQUENTIAL, "greeting-source-handler")
            .step("uppercase-message")
            .sinkHandlers(ExecutionMode.SEQUENTIAL, "audit-sink-handler")
            .sink("example-collector")
        );
    }
}
