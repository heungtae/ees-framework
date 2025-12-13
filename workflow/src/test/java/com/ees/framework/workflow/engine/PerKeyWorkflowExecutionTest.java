package com.ees.framework.workflow.engine;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.sink.Sink;
import com.ees.framework.source.Source;
import com.ees.framework.workflow.engine.WorkflowEngine.BackpressurePolicy;
import com.ees.framework.workflow.model.WorkflowEdgeDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.model.WorkflowNodeKind;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PerKeyWorkflowExecutionTest {

    @Test
    void preservesOrderPerKey() throws Exception {
        WorkflowGraphDefinition graph = linearGraph();
        List<FxContext<String>> contexts = List.of(
            context("eq-1", "a1"),
            context("eq-1", "a2"),
            context("eq-2", "b1"),
            context("eq-2", "b2")
        );
        Source<String> source = () -> contexts;
        TrackingSink sink = new TrackingSink(contexts.size());

        Workflow workflow = new WorkflowEngine().createWorkflow(graph, resolverFor(source, sink));
        workflow.start();

        assertThat(sink.await(Duration.ofSeconds(1))).isTrue();
        assertThat(sink.payloads("eq-1")).containsExactly("a1", "a2");
        assertThat(sink.payloads("eq-2")).containsExactly("b1", "b2");

        workflow.stop();
    }

    @Test
    void processesDifferentKeysInParallel() throws Exception {
        WorkflowGraphDefinition graph = linearGraph();
        List<FxContext<String>> contexts = List.of(
            context("eq-slow", "slow"),
            context("eq-fast", "fast")
        );
        CountDownLatch allowSlowSink = new CountDownLatch(1);
        CountDownLatch processedFast = new CountDownLatch(1);

        Source<String> source = () -> contexts;
        Sink<String> sink = context -> {
            if ("eq-slow".equals(context.affinity().value())) {
                try {
                    allowSlowSink.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                processedFast.countDown();
            }
        };

        Workflow workflow = new WorkflowEngine().createWorkflow(graph, resolverFor(source, sink));
        Thread runner = new Thread(workflow::start);
        runner.start();

        assertThat(processedFast.await(500, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(runner.isAlive()).isTrue();

        allowSlowSink.countDown();
        runner.join(2_000);
        workflow.stop();
    }

    @Test
    void dropsOldestWhenBackpressurePolicyIsDropOldest() throws Exception {
        WorkflowGraphDefinition graph = new WorkflowGraphDefinition(
            "per-key-drop",
            "source",
            Set.of("sink"),
            List.of(
                new WorkflowNodeDefinition("source", WorkflowNodeKind.SOURCE, "source-bean"),
                new WorkflowNodeDefinition("sink", WorkflowNodeKind.SINK, "sink-bean")
            ),
            List.of(new WorkflowEdgeDefinition("source", "sink", null)),
            new WorkflowEngine.BatchingOptions(1, 1, Duration.ofMillis(200), Duration.ofSeconds(1), BackpressurePolicy.DROP_OLDEST, false)
        );

        List<FxContext<String>> contexts = List.of(
            context("eq-1", "first"),
            context("eq-1", "second"),
            context("eq-1", "third")
        );
        Source<String> source = () -> contexts;
        CountDownLatch firstWritten = new CountDownLatch(1);
        Object monitor = new Object();
        boolean[] state = new boolean[] {false, false}; // [0]=blocking, [1]=release
        TrackingSink sink = new TrackingSink(contexts.size()) {
            @Override
            public void write(FxContext<String> context) {
                if ("first".equals(context.message().payload())) {
                    firstWritten.countDown();
                    synchronized (monitor) {
                        state[0] = true;
                        monitor.notifyAll();
                        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                        while (!state[1] && System.nanoTime() < deadlineNanos) {
                            try {
                                monitor.wait(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
                super.write(context);
            }
        };

        Workflow workflow = new WorkflowEngine().createWorkflow(graph, resolverFor(source, sink));
        Thread runner = new Thread(workflow::start);
        runner.start();

        assertThat(firstWritten.await(1, TimeUnit.SECONDS)).isTrue();
        synchronized (monitor) {
            long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (!state[0] && System.nanoTime() < deadlineNanos) {
                monitor.wait(50);
            }
            state[1] = true;
            monitor.notifyAll();
        }
        runner.join(2_000);
        workflow.stop();

        assertThat(sink.payloads("eq-1")).containsExactly("first", "third");
    }

    @Test
    void throwsWhenBackpressurePolicyIsError() throws Exception {
        WorkflowEngine.BatchingOptions options = new WorkflowEngine.BatchingOptions(
            1, 1, Duration.ofMillis(200), Duration.ofSeconds(1), BackpressurePolicy.ERROR, false
        );
        WorkflowGraphDefinition graph = new WorkflowGraphDefinition(
            "per-key-error",
            "source",
            Set.of("sink"),
            List.of(
                new WorkflowNodeDefinition("source", WorkflowNodeKind.SOURCE, "source-bean"),
                new WorkflowNodeDefinition("sink", WorkflowNodeKind.SINK, "sink-bean")
            ),
            List.of(new WorkflowEdgeDefinition("source", "sink", null)),
            options
        );
        List<FxContext<String>> contexts = List.of(
            context("eq-1", "first"),
            context("eq-1", "second"),
            context("eq-1", "third")
        );
        Source<String> source = () -> contexts;
        CountDownLatch releaseFirst = new CountDownLatch(1);
        Sink<String> sink = ctx -> {
            if ("first".equals(ctx.message().payload())) {
                awaitQuietly(releaseFirst);
            }
        };

        Workflow workflow = new WorkflowEngine().createWorkflow(graph, resolverFor(source, sink));
        Thread runner = new Thread(() -> assertThrows(IllegalStateException.class, workflow::start));
        runner.start();

        releaseFirst.countDown();
        runner.join(2_000);
        workflow.stop();
    }

    private WorkflowGraphDefinition linearGraph() {
        return new WorkflowGraphDefinition(
            "per-key",
            "source",
            Set.of("sink"),
            List.of(
                new WorkflowNodeDefinition("source", WorkflowNodeKind.SOURCE, "source-bean"),
                new WorkflowNodeDefinition("sink", WorkflowNodeKind.SINK, "sink-bean")
            ),
            List.of(new WorkflowEdgeDefinition("source", "sink", null))
        );
    }

    private WorkflowNodeResolver resolverFor(Source<String> source, Sink<String> sink) {
        Map<String, Object> beans = Map.of(
            "source-bean", source,
            "sink-bean", sink
        );
        return node -> {
            Object bean = beans.get(node.getRefName());
            if (bean == null) {
                throw new IllegalArgumentException("No bean for refName: " + node.getRefName());
            }
            return bean;
        };
    }

    private FxContext<String> context(String key, String payload) {
        return new FxContext<>(
            FxCommand.of("ingest"),
            FxHeaders.empty(),
            FxMessage.now("src", payload),
            FxMeta.empty(),
            FxAffinity.of("equipmentId", key)
        );
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class TrackingSink implements Sink<String> {

        private final Map<String, List<String>> payloadsByKey = new ConcurrentHashMap<>();
        private final CountDownLatch latch;

        TrackingSink(int expectedCount) {
            this.latch = new CountDownLatch(expectedCount);
        }

        @Override
        public void write(FxContext<String> context) {
            payloadsByKey.computeIfAbsent(context.affinity().value(), ignored -> new CopyOnWriteArrayList<>())
                .add(context.message().payload());
            latch.countDown();
        }

        boolean await(Duration timeout) throws InterruptedException {
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        List<String> payloads(String key) {
            return payloadsByKey.getOrDefault(key, List.of());
        }
    }
}
