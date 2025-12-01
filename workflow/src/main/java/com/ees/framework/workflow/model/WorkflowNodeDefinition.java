package com.ees.framework.workflow.model;

import java.util.Objects;

/**
 * Workflow 그래프 상의 노드 정의.
 */
public final class WorkflowNodeDefinition {

    private final String id;
    private final WorkflowNodeKind kind;
    private final String refName; // 실제 구현체를 찾을 때 사용할 이름/타입

    /**
     * 노드 정의를 생성한다.
     *
     * @param id      노드 ID (그래프 내 고유)
     * @param kind    노드 종류
     * @param refName 실제 구현체를 식별할 이름/타입
     */
    public WorkflowNodeDefinition(String id, WorkflowNodeKind kind, String refName) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.refName = Objects.requireNonNull(refName, "refName must not be null");
    }

    /** 노드 ID 반환 */
    public String getId() {
        return id;
    }

    /** 노드 종류 반환 */
    public WorkflowNodeKind getKind() {
        return kind;
    }

    /** 실제 구현체를 찾을 때 사용할 이름/타입 반환 */
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
