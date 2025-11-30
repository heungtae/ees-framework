package com.ees.framework.workflow.model;

import java.util.*;

/**
 * 그래프 기반 Workflow 정의.
 */
public final class WorkflowGraphDefinition {

    private final String name;
    private final String startNodeId;
    private final Set<String> endNodeIds;
    private final List<WorkflowNodeDefinition> nodes;
    private final List<WorkflowEdgeDefinition> edges;

    public WorkflowGraphDefinition(
        String name,
        String startNodeId,
        Set<String> endNodeIds,
        List<WorkflowNodeDefinition> nodes,
        List<WorkflowEdgeDefinition> edges
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.startNodeId = Objects.requireNonNull(startNodeId, "startNodeId must not be null");
        this.endNodeIds = Set.copyOf(endNodeIds);
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
    }

    public String getName() {
        return name;
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public Set<String> getEndNodeIds() {
        return Collections.unmodifiableSet(endNodeIds);
    }

    public List<WorkflowNodeDefinition> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<WorkflowEdgeDefinition> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public String toString() {
        return "WorkflowGraphDefinition{" +
            "name='" + name + '\'' +
            ", startNodeId='" + startNodeId + '\'' +
            ", endNodeIds=" + endNodeIds +
            ", nodes=" + nodes +
            ", edges=" + edges +
            '}';
    }
}
