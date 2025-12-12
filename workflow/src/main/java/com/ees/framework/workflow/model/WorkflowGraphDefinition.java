package com.ees.framework.workflow.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import com.ees.framework.workflow.engine.WorkflowEngine;

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
    WorkflowEngine.BatchingOptions batchingOptions;

    /**
     * 그래프 워크플로우 정의를 구성하는 생성자.
     *
     * @param name 워크플로우 이름
     * @param startNodeId 시작 노드 ID
     * @param endNodeIds 종료 노드 ID 집합
     * @param nodes 그래프 노드 목록
     * @param edges 그래프 엣지 목록
     * @param batchingOptions 배치/백프레셔 옵션 (null 이면 기본값 사용)
     */
    @Builder
    public WorkflowGraphDefinition(
        String name,
        String startNodeId,
        @Singular Set<String> endNodeIds,
        @Singular List<WorkflowNodeDefinition> nodes,
        @Singular("edge") List<WorkflowEdgeDefinition> edges,
        WorkflowEngine.BatchingOptions batchingOptions
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.startNodeId = Objects.requireNonNull(startNodeId, "startNodeId must not be null");
        this.endNodeIds = Set.copyOf(endNodeIds);
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.batchingOptions = batchingOptions == null
            ? WorkflowEngine.BatchingOptions.defaults()
            : batchingOptions;
    }

    /**
     * 기본 배치 옵션을 사용해 그래프 워크플로우 정의를 생성한다.
     *
     * @param name 워크플로우 이름
     * @param startNodeId 시작 노드 ID
     * @param endNodeIds 종료 노드 ID 집합
     * @param nodes 그래프 노드 목록
     * @param edges 그래프 엣지 목록
     */
    public WorkflowGraphDefinition(
        String name,
        String startNodeId,
        Set<String> endNodeIds,
        List<WorkflowNodeDefinition> nodes,
        List<WorkflowEdgeDefinition> edges
    ) {
        this(name, startNodeId, endNodeIds, nodes, edges, WorkflowEngine.BatchingOptions.defaults());
    }
}
