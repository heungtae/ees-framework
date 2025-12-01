package com.ees.framework.workflow.model;

import java.util.Objects;

/**
 * Workflow 그래프 상의 엣지 정의.
 */
public final class WorkflowEdgeDefinition {

    private final String fromNodeId;
    private final String toNodeId;
    private final String condition; // optional: 조건부로 엣지 선택 시 사용

    /**
     * 엣지 정의를 생성한다.
     *
     * @param fromNodeId 출발 노드 ID
     * @param toNodeId   도착 노드 ID
     * @param condition  조건식(없으면 null)
     */
    public WorkflowEdgeDefinition(String fromNodeId, String toNodeId, String condition) {
        this.fromNodeId = Objects.requireNonNull(fromNodeId, "fromNodeId must not be null");
        this.toNodeId = Objects.requireNonNull(toNodeId, "toNodeId must not be null");
        this.condition = condition;
    }

    /** 출발 노드 ID 반환 */
    public String getFromNodeId() {
        return fromNodeId;
    }

    /** 도착 노드 ID 반환 */
    public String getToNodeId() {
        return toNodeId;
    }

    /** 조건식 반환 (없으면 null) */
    public String getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "WorkflowEdgeDefinition{" +
            "fromNodeId='" + fromNodeId + '\'' +
            ", toNodeId='" + toNodeId + '\'' +
            ", condition='" + condition + '\'' +
            '}';
    }
}
