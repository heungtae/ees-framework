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

    /**
     * greeting 메시지를 AI 분류와 트리아지 sink까지 연결하는 데모 워크플로 정의를 등록한다.
     *
     * @return greeting 워크플로 정의
     */
    @Bean
    public WorkflowDefinition exampleGreetingWorkflow() {
        return WorkflowDsl.define("example-greeting-workflow", builder -> builder
            .source("example-greeting")
            .sourceHandlers(ExecutionMode.SEQUENTIAL, "greeting-source-handler")
            .step("uppercase-message")
            .step("ai-agent-step")
            .sinkHandlers(ExecutionMode.SEQUENTIAL, "audit-sink-handler")
            .sink("triage-collector")
        );
    }
}
