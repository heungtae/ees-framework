package com.ees.framework.workflow.model;

import java.util.Objects;

/**
 * Workflow 그래프 상의 노드 정의.
 */
public final class WorkflowNodeDefinition {

    private final String id;
    private final WorkflowNodeKind kind;
    private final String refName;

    public WorkflowNodeDefinition(String id, WorkflowNodeKind kind, String refName) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.refName = Objects.requireNonNull(refName, "refName must not be null");
    }

    public String getId() {
        return id;
    }

    public WorkflowNodeKind getKind() {
        return kind;
    }

    public String getRefName() {
        return refName;
    }

    @Override
    public String toString() {
        return "WorkflowNodeDefinition{" +
            "id='" + id + '\'' +
            ", kind=" + kind +
            ", refName='" + refName + '\'' +
            '}';
    }
}
