package com.ees.framework.workflow.model;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Workflow 그래프 상의 엣지 정의.
 */
@Value
public class WorkflowEdgeDefinition {

    /**
     * 출발 노드 ID.
     */
    String fromNodeId;

    /**
     * 도착 노드 ID.
     */
    String toNodeId;

    /**
     * 조건식(없으면 null).
     */
    String condition;

    @Builder
    public WorkflowEdgeDefinition(String fromNodeId, String toNodeId, String condition) {
        this.fromNodeId = Objects.requireNonNull(fromNodeId, "fromNodeId must not be null");
        this.toNodeId = Objects.requireNonNull(toNodeId, "toNodeId must not be null");
        this.condition = condition;
    }
}
