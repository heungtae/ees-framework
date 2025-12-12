package com.ees.framework.autoconfigure;

import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.annotations.FxSink;
import com.ees.framework.annotations.FxSource;
import com.ees.framework.annotations.SinkHandlerComponent;
import com.ees.framework.annotations.SourceHandlerComponent;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMessage;
import com.ees.framework.core.ExecutionMode;
import com.ees.framework.handlers.SinkHandler;
import com.ees.framework.handlers.SourceHandler;
import com.ees.framework.pipeline.PipelineStep;
import com.ees.framework.sink.Sink;
import com.ees.framework.source.Source;
import com.ees.framework.workflow.engine.WorkflowEngine;
import com.ees.framework.workflow.engine.Workflow;
import com.ees.framework.workflow.engine.WorkflowNodeResolver;
import com.ees.framework.workflow.engine.WorkflowRuntime;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.model.WorkflowNodeKind;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import com.ees.cluster.spring.ClusterAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class FxFrameworkAutoConfigurationWorkflowTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FxFrameworkAutoConfiguration.class, ClusterAutoConfiguration.class))
        .withUserConfiguration(SampleWorkflowConfig.class);

    @Test
    void connectsWorkflowModuleThroughAutoConfiguration() {
        contextRunner.run(context -> {
            WorkflowDefinition definition = context.getBean(WorkflowDefinition.class);
            LinearToGraphConverter converter = context.getBean(LinearToGraphConverter.class);
            WorkflowGraphDefinition graph = converter.convert(definition);

            context.getBean(WorkflowGraphValidator.class).validate(graph);

            WorkflowNodeResolver resolver = context.getBean(WorkflowNodeResolver.class);
            assertThat(resolver.resolve(nodeOfKind(graph, WorkflowNodeKind.SOURCE)))
                .isSameAs(context.getBean(SampleSource.class));
            assertThat(resolver.resolve(nodeOfKind(graph, WorkflowNodeKind.SOURCE_HANDLER)))
                .isSameAs(context.getBean(SampleSourceHandler.class));
            assertThat(resolver.resolve(nodeOfKind(graph, WorkflowNodeKind.PIPELINE_STEP)))
                .isSameAs(context.getBean(SamplePipelineStep.class));
            assertThat(resolver.resolve(nodeOfKind(graph, WorkflowNodeKind.SINK_HANDLER)))
                .isSameAs(context.getBean(SampleSinkHandler.class));
            assertThat(resolver.resolve(nodeOfKind(graph, WorkflowNodeKind.SINK)))
                .isSameAs(context.getBean(SampleSink.class));

            WorkflowEngine engine = context.getBean(WorkflowEngine.class);
            Workflow workflow = engine.createWorkflow(graph, resolver);
            assertThat(workflow.getName()).isEqualTo("sample-workflow");
            assertThat(graph.getStartNodeId()).isEqualTo("source");
            assertThat(graph.getEndNodeIds()).contains("sink");

            WorkflowRuntime runtime = context.getBean(WorkflowRuntime.class);
            assertThat(runtime.getWorkflows())
                .extracting(Workflow::getName)
                .containsExactly("sample-workflow");
        });
    }

    private WorkflowNodeDefinition nodeOfKind(
        WorkflowGraphDefinition graph,
        WorkflowNodeKind kind
    ) {
        return graph.getNodes().stream()
            .filter(node -> node.getKind() == kind)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No node for kind " + kind));
    }

    @TestConfiguration
    static class SampleWorkflowConfig {

        @Bean
        WorkflowDefinition sampleWorkflowDefinition() {
            return WorkflowDsl.define("sample-workflow", builder -> builder
                .source("sample-source")
                .sourceHandlers(ExecutionMode.SEQUENTIAL, "srcHandler")
                .step("enrich")
                .sinkHandlers(ExecutionMode.SEQUENTIAL, "sinkHandler")
                .sink("sample-sink")
            );
        }

        @Bean
        SampleSource sampleSource() {
            return new SampleSource();
        }

        @Bean
        SampleSourceHandler sampleSourceHandler() {
            return new SampleSourceHandler();
        }

        @Bean
        SamplePipelineStep samplePipelineStep() {
            return new SamplePipelineStep();
        }

        @Bean
        SampleSinkHandler sampleSinkHandler() {
            return new SampleSinkHandler();
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        SampleSink sampleSink() {
            return new SampleSink();
        }
    }

    @FxSource(type = "sample-source")
    static class SampleSource implements Source<String> {
        @Override
        public Iterable<FxContext<String>> read() {
            FxMessage<String> message = FxMessage.now("sample-source", "payload");
            return java.util.List.of(FxContext.of(message, FxCommand.of("ingest")));
        }
    }

    @SourceHandlerComponent("srcHandler")
    static class SampleSourceHandler implements SourceHandler<String> {
        @Override
        public FxContext<String> handle(FxContext<String> context) {
            return context;
        }
    }

    @FxPipelineStep("enrich")
    static class SamplePipelineStep implements PipelineStep<String, String> {
        @Override
        public FxContext<String> apply(FxContext<String> context) {
            return context;
        }
    }

    @SinkHandlerComponent("sinkHandler")
    static class SampleSinkHandler implements SinkHandler<String> {
        @Override
        public FxContext<String> handle(FxContext<String> context) {
            return context;
        }
    }

    @FxSink("sample-sink")
    static class SampleSink implements Sink<String> {
        @Override
        public void write(FxContext<String> context) {
        }
    }
}
