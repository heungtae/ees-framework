package com.ees.framework.workflow.model;

import java.util.Objects;

/**
 * Workflow 그래프 상의 엣지 정의.
 */
public final class WorkflowEdgeDefinition {

    private final String fromNodeId;
    private final String toNodeId;
    private final String condition; // optional

    public WorkflowEdgeDefinition(String fromNodeId, String toNodeId, String condition) {
        this.fromNodeId = Objects.requireNonNull(fromNodeId, "fromNodeId must not be null");
        this.toNodeId = Objects.requireNonNull(toNodeId, "toNodeId must not be null");
        this.condition = condition;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

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
