package com.ees.framework.example.config;

import com.ees.framework.core.ExecutionMode;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 예시 AI 워크플로 정의를 등록한다.
 */
@Configuration
@Import({
    com.ees.framework.example.ai.AiWorkflowSource.class,
    com.ees.framework.example.ai.AiResultSink.class,
    com.ees.framework.example.ai.StubAiAgentService.class
})
public class AiWorkflowConfiguration {
    /**
     * aiDemoWorkflowDefinition를 수행한다.
     * @return 
     */

    @Bean
    public WorkflowDefinition aiDemoWorkflowDefinition() {
        return WorkflowDsl.define("example-ai-workflow", builder -> builder
            .source("ai-demo-source")
            .step("ai-agent-step")
            .sink("ai-demo-collector")
        );
    }
}
