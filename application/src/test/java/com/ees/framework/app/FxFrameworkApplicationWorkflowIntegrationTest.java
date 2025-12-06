package com.ees.framework.app;

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
import com.ees.framework.workflow.engine.WorkflowRuntime;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FxFrameworkApplication.class)
@Import(FxFrameworkApplicationWorkflowIntegrationTest.WorkflowSampleConfig.class)
class FxFrameworkApplicationWorkflowIntegrationTest {

    @Autowired
    WorkflowRuntime workflowRuntime;

    @Autowired
    RecordingSink recordingSink;

    @Autowired
    AuditSourceHandler auditSourceHandler;

    @Autowired
    TrackingSinkHandler trackingSinkHandler;

    @AfterEach
    void tearDown() {
        workflowRuntime.stopAll().block();
    }

    @Test
    void runsWorkflowEndToEndThroughSpringApplication() throws Exception {
        assertThat(workflowRuntime.getWorkflow("spring-boot-workflow")).isPresent();

        workflowRuntime.startAll().block();

        assertThat(recordingSink.awaitWrite()).isTrue();
        FxContext<String> captured = recordingSink.getReceived();
        assertThat(captured.message().payload()).isEqualTo("HELLO-handled");
        assertThat(auditSourceHandler.invocations()).isEqualTo(1);
        assertThat(trackingSinkHandler.invocations()).isEqualTo(1);
    }

    @TestConfiguration
    @Import({
        SampleSource.class,
        AuditSourceHandler.class,
        UppercasePipelineStep.class,
        TrackingSinkHandler.class,
        RecordingSink.class
    })
    static class WorkflowSampleConfig {

        @Bean
        WorkflowDefinition springBootWorkflowDefinition() {
            return WorkflowDsl.define(
                "spring-boot-workflow",
                builder -> builder
                    .source("spring-source")
                    .sourceHandlers(ExecutionMode.SEQUENTIAL, "audit-source-handler")
                    .step("uppercase-step")
                    .sinkHandlers(ExecutionMode.SEQUENTIAL, "tracking-sink-handler")
                    .sink("spring-sink")
            );
        }
    }

    @FxSource(type = "spring-source")
    static class SampleSource implements Source<String> {
        @Override
        public Flux<FxContext<String>> read() {
            FxMessage<String> message = FxMessage.now("spring-source", "hello");
            return Flux.just(FxContext.of(message, FxCommand.of("ingest")));
        }
    }

    @SourceHandlerComponent("audit-source-handler")
    static class AuditSourceHandler implements SourceHandler<String> {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public Mono<FxContext<String>> handle(FxContext<String> context) {
            invocations.incrementAndGet();
            return Mono.just(context);
        }

        int invocations() {
            return invocations.get();
        }
    }

    @FxPipelineStep("uppercase-step")
    static class UppercasePipelineStep implements PipelineStep<String, String> {
        @Override
        public Mono<FxContext<String>> apply(FxContext<String> context) {
            FxMessage<String> uppercased = new FxMessage<>(
                context.message().sourceType(),
                context.message().payload().toUpperCase(),
                context.message().timestamp(),
                context.message().key()
            );
            FxContext<String> updated = new FxContext<>(
                context.command(),
                context.headers(),
                uppercased,
                context.meta()
            );
            return Mono.just(updated);
        }
    }

    @SinkHandlerComponent("tracking-sink-handler")
    static class TrackingSinkHandler implements SinkHandler<String> {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public Mono<FxContext<String>> handle(FxContext<String> context) {
            invocations.incrementAndGet();
            FxMessage<String> decorated = new FxMessage<>(
                context.message().sourceType(),
                context.message().payload() + "-handled",
                context.message().timestamp(),
                context.message().key()
            );
            FxContext<String> updated = new FxContext<>(
                context.command(),
                context.headers(),
                decorated,
                context.meta()
            );
            return Mono.just(updated);
        }

        int invocations() {
            return invocations.get();
        }
    }

    @FxSink("spring-sink")
    static class RecordingSink implements Sink<String> {

        private final CountDownLatch latch = new CountDownLatch(1);
        private FxContext<String> received;

        @Override
        public Mono<Void> write(FxContext<String> context) {
            this.received = context;
            latch.countDown();
            return Mono.empty();
        }

        boolean awaitWrite() throws InterruptedException {
            return latch.await(1, TimeUnit.SECONDS);
        }

        FxContext<String> getReceived() {
            return received;
        }
    }
}
