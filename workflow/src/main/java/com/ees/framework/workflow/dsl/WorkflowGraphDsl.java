package com.ees.framework.workflow.dsl;

import com.ees.framework.workflow.model.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * 그래프 기반 Workflow 정의 DSL.
 */
public final class WorkflowGraphDsl {

    private WorkflowGraphDsl() {
    }

    public static WorkflowGraphDefinition define(String name, Consumer<Builder> spec) {
        Builder builder = new Builder(name);
        spec.accept(builder);
        return builder.build();
    }

    public static final class Builder {

        private final String name;
        private final Map<String, WorkflowNodeDefinition> nodes = new LinkedHashMap<>();
        private final List<WorkflowEdgeDefinition> edges = new ArrayList<>();
        private String startNodeId;
        private final Set<String> endNodeIds = new LinkedHashSet<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder source(String id, String sourceType) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SOURCE, sourceType);
            nodes.put(id, node);
            if (startNodeId == null) {
                startNodeId = id;
            }
            return this;
        }

        public Builder sourceHandler(String id, String handlerName) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SOURCE_HANDLER, handlerName);
            nodes.put(id, node);
            return this;
        }

        public Builder pipelineStep(String id, String stepName) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.PIPELINE_STEP, stepName);
            nodes.put(id, node);
            return this;
        }

        public Builder sinkHandler(String id, String handlerName) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SINK_HANDLER, handlerName);
            nodes.put(id, node);
            return this;
        }

        public Builder sink(String id, String sinkType) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SINK, sinkType);
            nodes.put(id, node);
            endNodeIds.add(id);
            return this;
        }

        public Builder edge(String fromId, String toId) {
            return edge(fromId, toId, null);
        }

        public Builder edge(String fromId, String toId, String condition) {
            WorkflowEdgeDefinition edge = new WorkflowEdgeDefinition(fromId, toId, condition);
            edges.add(edge);
            endNodeIds.remove(fromId);
            return this;
        }

        public WorkflowGraphDefinition build() {
            if (startNodeId == null) {
                throw new IllegalStateException("startNodeId is not set (no source node?)");
            }
            if (endNodeIds.isEmpty()) {
                inferEndNodesFromSinks();
            }
            return new WorkflowGraphDefinition(
                name,
                startNodeId,
                endNodeIds,
                new ArrayList<>(nodes.values()),
                edges
            );
        }

        private void inferEndNodesFromSinks() {
            for (WorkflowNodeDefinition node : nodes.values()) {
                if (node.getKind() == WorkflowNodeKind.SINK) {
                    endNodeIds.add(node.getId());
                }
            }
        }
    }
}
