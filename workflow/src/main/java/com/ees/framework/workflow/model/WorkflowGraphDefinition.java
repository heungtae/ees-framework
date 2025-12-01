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

    /**
     * 그래프 기반 워크플로우 정의를 생성한다.
     *
     * @param name        워크플로우 이름
     * @param startNodeId 시작 노드 ID
     * @param endNodeIds  종료 노드 ID 집합
     * @param nodes       그래프 노드 목록
     * @param edges       그래프 엣지 목록
     */
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

    /** 워크플로우 이름 */
    public String getName() {
        return name;
    }

    /** 시작 노드 ID */
    public String getStartNodeId() {
        return startNodeId;
    }

    /** 종료 노드 ID 집합 */
    public Set<String> getEndNodeIds() {
        return Collections.unmodifiableSet(endNodeIds);
    }

    /** 노드 목록 */
    public List<WorkflowNodeDefinition> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /** 엣지 목록 */
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
