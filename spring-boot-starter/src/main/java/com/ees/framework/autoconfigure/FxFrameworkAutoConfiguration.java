package com.ees.framework.autoconfigure;

import com.ees.cluster.spring.ClusterProperties;
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
import com.ees.framework.workflow.engine.WorkflowEngine;
import com.ees.framework.workflow.engine.WorkflowRuntime;
import com.ees.framework.workflow.engine.WorkflowNodeResolver;
import com.ees.framework.workflow.affinity.DefaultAffinityKeyResolver;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import com.ees.framework.workflow.WorkflowProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * FX Framework Spring Boot AutoConfiguration.
 *
 * - 레지스트리 Bean 생성
 * - WorkflowGraphValidator / LinearToGraphConverter / WorkflowEngine Bean 생성
 * - WorkflowNodeResolver 기본 구현 생성
 * - WorkflowDefinition / WorkflowGraphDefinition 리스트는 추후 properties/DSL 로 확장
 */
@AutoConfiguration
@EnableConfigurationProperties({ClusterProperties.class, WorkflowProperties.class})
public class FxFrameworkAutoConfiguration {

    // ------------------------------------------------------------------------
    // Registry beans
    // ------------------------------------------------------------------------
    /**
     * sourceRegistry를 수행한다.
     * @param sources 
     * @return 
     */

    @Bean
    public SourceRegistry sourceRegistry(List<Source<?>> sources) {
        return new DefaultSourceRegistry(sources);
    }
    /**
     * sourceHandlerRegistry를 수행한다.
     * @param handlers 
     * @return 
     */

    @Bean
    public SourceHandlerRegistry sourceHandlerRegistry(List<SourceHandler<?>> handlers) {
        return new DefaultSourceHandlerRegistry(handlers);
    }
    /**
     * sinkHandlerRegistry를 수행한다.
     * @param handlers 
     * @return 
     */

    @Bean
    public SinkHandlerRegistry sinkHandlerRegistry(List<SinkHandler<?>> handlers) {
        return new DefaultSinkHandlerRegistry(handlers);
    }
    /**
     * pipelineStepRegistry를 수행한다.
     * @param steps 
     * @return 
     */

    @Bean
    public PipelineStepRegistry pipelineStepRegistry(List<PipelineStep<?, ?>> steps) {
        return new DefaultPipelineStepRegistry(steps);
    }
    /**
     * sinkRegistry를 수행한다.
     * @param sinks 
     * @return 
     */

    @Bean
    public SinkRegistry sinkRegistry(List<Sink<?>> sinks) {
        return new DefaultSinkRegistry(sinks);
    }

    // ------------------------------------------------------------------------
    // Workflow utilities
    // ------------------------------------------------------------------------
    /**
     * workflowGraphValidator를 수행한다.
     * @return 
     */

    @Bean
    public WorkflowGraphValidator workflowGraphValidator() {
        return new WorkflowGraphValidator();
    }
    /**
     * linearToGraphConverter를 수행한다.
     * @return 
     */

    @Bean
    public LinearToGraphConverter linearToGraphConverter() {
        return new LinearToGraphConverter();
    }
    /**
     * reactorWorkflowEngine를 수행한다.
     * @param clusterProperties 
     * @param workflowProperties 
     * @return 
     */

    @Bean
    public WorkflowEngine reactorWorkflowEngine(ClusterProperties clusterProperties,
                                                        WorkflowProperties workflowProperties) {
        return new WorkflowEngine(
            workflowProperties.toBatchingOptions(),
            new DefaultAffinityKeyResolver(clusterProperties.getAssignmentAffinityKind())
        );
    }
    /**
     * workflowNodeResolver를 수행한다.
     * @param sourceRegistry 
     * @param sourceHandlerRegistry 
     * @param pipelineStepRegistry 
     * @param sinkHandlerRegistry 
     * @param sinkRegistry 
     * @return 
     */

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
    // Workflow runtime
    // ------------------------------------------------------------------------
    /**
     * workflowRuntime를 수행한다.
     * @param workflowDefinitions 
     * @param workflowGraphDefinitions 
     * @param converter 
     * @param validator 
     * @param engine 
     * @param resolver 
     * @return 
     */

    @Bean
    public WorkflowRuntime workflowRuntime(
        ObjectProvider<WorkflowDefinition> workflowDefinitions,
        ObjectProvider<WorkflowGraphDefinition> workflowGraphDefinitions,
        LinearToGraphConverter converter,
        WorkflowGraphValidator validator,
        WorkflowEngine engine,
        WorkflowNodeResolver resolver
    ) {
        return new WorkflowRuntime(
            workflowDefinitions.orderedStream().toList(),
            workflowGraphDefinitions.orderedStream().toList(),
            converter,
            validator,
            engine,
            resolver
        );
    }
}
