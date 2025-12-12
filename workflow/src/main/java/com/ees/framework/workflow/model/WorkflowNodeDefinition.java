package com.ees.framework.workflow.model;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Workflow 그래프 상의 노드 정의.
 */
@Value
public class WorkflowNodeDefinition {

    /**
     * 노드 ID (그래프 내 고유).
     */
    String id;

    /**
     * 노드 종류.
     */
    WorkflowNodeKind kind;

    /**
     * 실제 구현체를 찾을 때 사용할 이름/타입.
     */
    String refName;

    /**
     * 노드 ID, 종류, 참조 이름을 지정해 노드 정의를 생성한다.
     *
     * @param id 그래프 내 고유 ID
     * @param kind 노드 종류
     * @param refName 실제 구현체를 식별할 참조 이름
     */
    @Builder
    public WorkflowNodeDefinition(String id, WorkflowNodeKind kind, String refName) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.refName = Objects.requireNonNull(refName, "refName must not be null");
    }
}
