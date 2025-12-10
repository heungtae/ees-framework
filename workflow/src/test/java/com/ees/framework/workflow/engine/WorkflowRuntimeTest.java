package com.ees.framework.workflow.engine;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.dsl.WorkflowGraphDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRuntimeTest {

    @Test
    void runsMultipleWorkflowAndGraphDefinitions() throws Exception {
        WorkflowDefinition alpha = WorkflowDsl.define("alpha", builder -> builder
            .source("shared-source")
            .sink("sink-a")
        );
        WorkflowDefinition beta = WorkflowDsl.define("beta", builder -> builder
            .source("shared-source")
            .sink("sink-b")
        );
        WorkflowGraphDefinition graphOnly = WorkflowGraphDsl.define("graph-only", builder -> builder
            .source("src", "shared-source")
            .sink("snk", "sink-c")
            .edge("src", "snk")
        );

        CountingSource source = new CountingSource(3);
        CountingSink sinkA = new CountingSink();
        CountingSink sinkB = new CountingSink();
        CountingSink sinkC = new CountingSink();

        WorkflowRuntime runtime = new WorkflowRuntime(
            List.of(alpha, beta),
            List.of(graphOnly),
            new LinearToGraphConverter(),
            new WorkflowGraphValidator(),
            new BlockingWorkflowEngine(),
            new StaticResolver(Map.of(
                "shared-source", source,
                "sink-a", sinkA,
                "sink-b", sinkB,
                "sink-c", sinkC
            ))
        );

        assertThat(runtime.getWorkflows())
            .extracting(Workflow::getName)
            .containsExactly("alpha", "beta", "graph-only");

        runtime.startAll();

        assertThat(source.awaitStarts()).isTrue();
        assertThat(sinkA.awaitWrites()).isTrue();
        assertThat(sinkB.awaitWrites()).isTrue();
        assertThat(sinkC.awaitWrites()).isTrue();

        runtime.stopAll();
    }

    private static class StaticResolver implements WorkflowNodeResolver {

        private final Map<String, Object> beans;

        StaticResolver(Map<String, Object> beans) {
            this.beans = beans;
        }

        @Override
        public Object resolve(WorkflowNodeDefinition node) {
            Object bean = beans.get(node.getRefName());
            if (bean == null) {
                throw new IllegalArgumentException("No bean for refName: " + node.getRefName());
            }
            return bean;
        }
    }

    private static class CountingSource implements com.ees.framework.source.Source<String> {

        private final CountDownLatch latch;

        CountingSource(int expectedStarts) {
            this.latch = new CountDownLatch(expectedStarts);
        }

        @Override
        public Iterable<FxContext<String>> read() {
            latch.countDown();
            FxMessage<String> message = FxMessage.now("shared-source", "payload");
            return List.of(new FxContext<>(
                FxCommand.of("ingest"),
                FxHeaders.empty(),
                message,
                FxMeta.empty(),
                FxAffinity.of("equipmentId", "eq-1")
            ));
        }

        boolean awaitStarts() throws InterruptedException {
            return latch.await(1, TimeUnit.SECONDS);
        }
    }

    private static class CountingSink implements com.ees.framework.sink.Sink<String> {

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void write(FxContext<String> context) {
            latch.countDown();
        }

        boolean awaitWrites() throws InterruptedException {
            return latch.await(1, TimeUnit.SECONDS);
        }
    }
}
