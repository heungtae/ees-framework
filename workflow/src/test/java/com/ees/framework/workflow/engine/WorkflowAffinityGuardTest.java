package com.ees.framework.workflow.engine;

import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.workflow.affinity.DefaultAffinityKeyResolver;
import com.ees.framework.workflow.model.WorkflowEdgeDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.model.WorkflowNodeKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowAffinityGuardTest {

    @Test
    void failsWhenAffinityMissing() {
        WorkflowGraphDefinition graph = new WorkflowGraphDefinition(
            "affinity-guard",
            "source",
            Set.of("sink"),
            List.of(
                new WorkflowNodeDefinition("source", WorkflowNodeKind.SOURCE, "src"),
                new WorkflowNodeDefinition("sink", WorkflowNodeKind.SINK, "snk")
            ),
            List.of(new WorkflowEdgeDefinition("source", "sink", null))
        );

        WorkflowEngine engine = new WorkflowEngine();
        Workflow workflow = engine.createWorkflow(graph, new SimpleResolver());

        assertThrows(IllegalStateException.class, workflow::start);
    }

    @Test
    void failsWhenAffinityKindDiffersFromDefault() {
        WorkflowGraphDefinition graph = new WorkflowGraphDefinition(
            "affinity-kind-guard",
            "source",
            Set.of("sink"),
            List.of(
                new WorkflowNodeDefinition("source", WorkflowNodeKind.SOURCE, "src"),
                new WorkflowNodeDefinition("sink", WorkflowNodeKind.SINK, "snk")
            ),
            List.of(new WorkflowEdgeDefinition("source", "sink", null))
        );

        WorkflowEngine engine = new WorkflowEngine(
            WorkflowEngine.BatchingOptions.defaults(),
            new DefaultAffinityKeyResolver("equipmentId")
        );
        Workflow workflow = engine.createWorkflow(graph, new MismatchedResolver());

        assertThrows(IllegalStateException.class, workflow::start);
    }

    private static final class SimpleResolver implements WorkflowNodeResolver {
        @Override
        public Object resolve(WorkflowNodeDefinition node) {
            return switch (node.getKind()) {
                case SOURCE -> (com.ees.framework.source.Source<Object>) () -> List.of(
                    new FxContext<>(
                        FxCommand.of("cmd"),
                        FxHeaders.empty(),
                        new FxMessage<>("src", "payload", Instant.now(), null),
                        FxMeta.empty(),
                        com.ees.framework.context.FxAffinity.none()
                    )
                );
                case SINK -> (com.ees.framework.sink.Sink<Object>) ctx -> { };
                default -> throw new IllegalStateException("Unsupported kind for test: " + node.getKind());
            };
        }
    }

    private static final class MismatchedResolver implements WorkflowNodeResolver {
        @Override
        public Object resolve(WorkflowNodeDefinition node) {
            return switch (node.getKind()) {
                case SOURCE -> (com.ees.framework.source.Source<Object>) () -> List.of(
                    new FxContext<>(
                        FxCommand.of("cmd"),
                        FxHeaders.empty(),
                        new FxMessage<>("src", "payload", Instant.now(), null),
                        FxMeta.empty(),
                        com.ees.framework.context.FxAffinity.of("lotId", "lot-1")
                    )
                );
                case SINK -> (com.ees.framework.sink.Sink<Object>) ctx -> { };
                default -> throw new IllegalStateException("Unsupported kind for test: " + node.getKind());
            };
        }
    }
}
