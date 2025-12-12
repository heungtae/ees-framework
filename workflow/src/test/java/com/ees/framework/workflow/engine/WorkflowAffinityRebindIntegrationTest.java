package com.ees.framework.workflow.engine;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.sink.Sink;
import com.ees.framework.source.Source;
import com.ees.framework.workflow.affinity.AffinityKindChangeHandler;
import com.ees.framework.workflow.dsl.WorkflowDsl;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.model.WorkflowNodeKind;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowAffinityRebindIntegrationTest {

    @Test
    void rebindsWorkflowWhenAffinityKindChanges() throws Exception {
        // given: runtime with default equipmentId kind and simple source->sink workflow
        MutableSource source = new MutableSource();
        ResettableSink sink = new ResettableSink();
        WorkflowDefinition def = WorkflowDsl.define("affinity-rebind", builder -> builder
            .source("src-bean")
            .sink("sink-bean")
        );

        WorkflowEngine engine = new WorkflowEngine();
        WorkflowRuntime runtime = buildRuntime(def, source, sink, engine);
        AffinityKindChangeHandler handler = AffinityKindChangeHandler.forRuntime(engine, runtime);

        // first run: equipmentId contexts are processed
        source.set(List.of(context("equipmentId", "eq-1", "payload-1")));
        sink.expect(1);

        runtime.startAll();

        assertThat(sink.await(Duration.ofSeconds(1))).isTrue();
        assertThat(sink.lastAffinity().kind()).isEqualTo("equipmentId");

        // when: affinity kind switches to lotId and source emits lotId keys
        sink.expect(1);
        sink.clear();
        source.set(List.of(context("lotId", "lot-1", "payload-2")));

        handler.onAffinityKindChanged("lotId");

        // then: workflows are rebound and accept the new affinity kind
        assertThat(sink.await(Duration.ofSeconds(1))).isTrue();
        assertThat(sink.lastAffinity().kind()).isEqualTo("lotId");

        runtime.stopAll();
    }

    private WorkflowRuntime buildRuntime(WorkflowDefinition definition, Source<String> source, Sink<String> sink, WorkflowEngine engine) {
        WorkflowGraphDefinition graph = new LinearToGraphConverter().convert(definition);
        WorkflowGraphValidator validator = new WorkflowGraphValidator();
        WorkflowNodeResolver resolver = resolverFor(source, sink);
        return new WorkflowRuntime(
            List.of(),
            List.of(graph),
            new LinearToGraphConverter(),
            validator,
            engine,
            resolver
        );
    }

    private WorkflowNodeResolver resolverFor(Source<String> source, Sink<String> sink) {
        Map<String, Object> beans = Map.of(
            "src-bean", source,
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

    private FxContext<String> context(String kind, String key, String payload) {
        return new FxContext<>(
            FxCommand.of("ingest"),
            FxHeaders.empty(),
            FxMessage.now("src", payload),
            FxMeta.empty(),
            FxAffinity.of(kind, key)
        );
    }

    private static class MutableSource implements Source<String> {

        private final AtomicReference<List<FxContext<String>>> contexts = new AtomicReference<>(List.of());

        void set(List<FxContext<String>> values) {
            contexts.set(values);
        }

        @Override
        public Iterable<FxContext<String>> read() {
            return contexts.get();
        }
    }

    private static class ResettableSink implements Sink<String> {

        private final AtomicReference<CountDownLatch> latchRef = new AtomicReference<>();
        private final CopyOnWriteArrayList<FxContext<String>> seen = new CopyOnWriteArrayList<>();

        void expect(int count) {
            latchRef.set(new CountDownLatch(count));
        }

        boolean await(Duration timeout) throws InterruptedException {
            CountDownLatch latch = latchRef.get();
            if (latch == null) {
                return false;
            }
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void clear() {
            seen.clear();
        }

        FxAffinity lastAffinity() {
            if (seen.isEmpty()) {
                return FxAffinity.none();
            }
            return seen.get(seen.size() - 1).affinity();
        }

        @Override
        public void write(FxContext<String> context) {
            seen.add(context);
            CountDownLatch latch = latchRef.get();
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
