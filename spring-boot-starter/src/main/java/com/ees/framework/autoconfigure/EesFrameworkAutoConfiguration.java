package com.ees.framework.autoconfigure;

import com.ees.framework.handlers.SinkHandler;
import com.ees.framework.handlers.SourceHandler;
import com.ees.framework.pipeline.PipelineStep;
import com.ees.framework.registry.DefaultPipelineStepRegistry;
import com.ees.framework.registry.DefaultSinkHandlerRegistry;
import com.ees.framework.registry.DefaultSinkRegistry;
import com.ees.framework.registry.DefaultSourceHandlerRegistry;
import com.ees.framework.registry.DefaultSourceRegistry;
import com.ees.framework.registry.PipelineStepRegistry;
import com.ees.framework.registry.SinkHandlerRegistry;
import com.ees.framework.registry.SinkRegistry;
import com.ees.framework.registry.SourceHandlerRegistry;
import com.ees.framework.registry.SourceRegistry;
import com.ees.framework.sink.Sink;
import com.ees.framework.source.Source;
import com.ees.framework.workflow.DefaultWorkflowNodeResolver;
import com.ees.framework.workflow.engine.ReactorWorkflowEngine;
import com.ees.framework.workflow.engine.WorkflowNodeResolver;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;

/**
 * EES Framework Spring Boot AutoConfiguration.
 *
 * - 레지스트리 Bean 생성
 * - WorkflowGraphValidator / LinearToGraphConverter / ReactorWorkflowEngine Bean 생성
 * - WorkflowNodeResolver 기본 구현 생성
 * - WorkflowDefinition / WorkflowGraphDefinition 리스트는 추후 properties/DSL 로 확장
 */
@AutoConfiguration
public class EesFrameworkAutoConfiguration {

    // ------------------------------------------------------------------------
    // Registry beans
    // ------------------------------------------------------------------------

    @Bean
    public SourceRegistry sourceRegistry(List<Source<?>> sources) {
        return new DefaultSourceRegistry(sources);
    }

    @Bean
    public SourceHandlerRegistry sourceHandlerRegistry(List<SourceHandler<?>> handlers) {
        return new DefaultSourceHandlerRegistry(handlers);
    }

    @Bean
    public SinkHandlerRegistry sinkHandlerRegistry(List<SinkHandler<?>> handlers) {
        return new DefaultSinkHandlerRegistry(handlers);
    }

    @Bean
    public PipelineStepRegistry pipelineStepRegistry(List<PipelineStep<?, ?>> steps) {
        return new DefaultPipelineStepRegistry(steps);
    }

    @Bean
    public SinkRegistry sinkRegistry(List<Sink<?>> sinks) {
        return new DefaultSinkRegistry(sinks);
    }

    // ------------------------------------------------------------------------
    // Workflow utilities
    // ------------------------------------------------------------------------

    @Bean
    public WorkflowGraphValidator workflowGraphValidator() {
        return new WorkflowGraphValidator();
    }

    @Bean
    public LinearToGraphConverter linearToGraphConverter() {
        return new LinearToGraphConverter();
    }

    @Bean
    public ReactorWorkflowEngine reactorWorkflowEngine() {
        return new ReactorWorkflowEngine();
    }

    @Bean
    public WorkflowNodeResolver workflowNodeResolver(
        SourceRegistry sourceRegistry,
        SourceHandlerRegistry sourceHandlerRegistry,
        PipelineStepRegistry pipelineStepRegistry,
        SinkHandlerRegistry sinkHandlerRegistry,
        SinkRegistry sinkRegistry
    ) {
        return new DefaultWorkflowNodeResolver(
            sourceRegistry,
            sourceHandlerRegistry,
            pipelineStepRegistry,
            sinkHandlerRegistry,
            sinkRegistry
        );
    }

    // ------------------------------------------------------------------------
    // WorkflowDefinition / GraphDefinition placeholder beans
    // (실제 프로젝트에서 properties/DSL 로 대체/확장)
    // ------------------------------------------------------------------------

    @Bean
    public List<WorkflowDefinition> workflowDefinitions() {
        return Collections.emptyList();
    }

    @Bean
    public List<WorkflowGraphDefinition> workflowGraphDefinitions() {
        return Collections.emptyList();
    }
}
