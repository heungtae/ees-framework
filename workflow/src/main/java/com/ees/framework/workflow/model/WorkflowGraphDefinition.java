package com.ees.framework.workflow.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import com.ees.framework.workflow.engine.BlockingWorkflowEngine;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 그래프 기반 Workflow 정의.
 */
@Value
public class WorkflowGraphDefinition {

    /**
     * 워크플로우 이름.
     */
    String name;

    /**
     * 시작 노드 ID.
     */
    String startNodeId;

    /**
     * 종료 노드 ID 집합.
     */
    Set<String> endNodeIds;

    /**
     * 그래프 노드 목록.
     */
    List<WorkflowNodeDefinition> nodes;

    /**
     * 그래프 엣지 목록.
     */
    List<WorkflowEdgeDefinition> edges;

    /**
     * 워크플로 실행 옵션(배치/백프레셔 등).
     */
    BlockingWorkflowEngine.BatchingOptions batchingOptions;

    @Builder
    public WorkflowGraphDefinition(
        String name,
        String startNodeId,
        @Singular Set<String> endNodeIds,
        @Singular List<WorkflowNodeDefinition> nodes,
        @Singular("edge") List<WorkflowEdgeDefinition> edges,
        BlockingWorkflowEngine.BatchingOptions batchingOptions
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.startNodeId = Objects.requireNonNull(startNodeId, "startNodeId must not be null");
        this.endNodeIds = Set.copyOf(endNodeIds);
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.batchingOptions = batchingOptions == null
            ? BlockingWorkflowEngine.BatchingOptions.defaults()
            : batchingOptions;
    }

    public WorkflowGraphDefinition(
        String name,
        String startNodeId,
        Set<String> endNodeIds,
        List<WorkflowNodeDefinition> nodes,
        List<WorkflowEdgeDefinition> edges
    ) {
        this(name, startNodeId, endNodeIds, nodes, edges, BlockingWorkflowEngine.BatchingOptions.defaults());
    }
}
