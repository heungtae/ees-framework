package com.ees.framework.workflow.dsl;

import com.ees.framework.workflow.model.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * 그래프 기반 Workflow 정의 DSL.
 */
public final class WorkflowGraphDsl {
    // 인스턴스를 생성한다.

    private WorkflowGraphDsl() {
    }

    /**
     * 그래프 기반 워크플로우를 DSL로 정의한다.
     *
     * @param name 워크플로우 이름
     * @param spec 빌더 설정 람다
     * @return 완성된 WorkflowGraphDefinition
     */
    public static WorkflowGraphDefinition define(String name, Consumer<Builder> spec) {
        Builder builder = new Builder(name);
        spec.accept(builder);
        return builder.build();
    }

    public static final class Builder {

        private final String name;
        private final Map<String, WorkflowNodeDefinition> nodes = new LinkedHashMap<>();
        private final List<WorkflowEdgeDefinition> edges = new ArrayList<>();
        private String startNodeId;
        private final Set<String> endNodeIds = new LinkedHashSet<>();
        private com.ees.framework.workflow.engine.WorkflowEngine.BatchingOptions batchingOptions;

        /**
         * 그래프 워크플로우 빌더를 생성한다.
         *
         * @param name 워크플로우 이름
         */
        public Builder(String name) {
            this.name = name;
        }

        /**
         * 시작 노드로 사용될 Source 정의를 추가한다. 처음 추가된 소스가 기본 start 노드가 된다.
         *
         * @param id 노드 ID
         * @param sourceType Source 타입 이름
         * @return 빌더
         */
        public Builder source(String id, String sourceType) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SOURCE, sourceType);
            nodes.put(id, node);
            if (startNodeId == null) {
                startNodeId = id;
            }
            return this;
        }

        /**
         * Source 단계에 연결될 핸들러 노드를 정의한다.
         *
         * @param id 노드 ID
         * @param handlerName 핸들러 이름
         * @return 빌더
         */
        public Builder sourceHandler(String id, String handlerName) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SOURCE_HANDLER, handlerName);
            nodes.put(id, node);
            return this;
        }

        /**
         * 파이프라인 변환 단계 노드를 정의한다.
         *
         * @param id 노드 ID
         * @param stepName 파이프라인 스텝 이름
         * @return 빌더
         */
        public Builder pipelineStep(String id, String stepName) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.PIPELINE_STEP, stepName);
            nodes.put(id, node);
            return this;
        }

        /**
         * Sink 전달 전/후에 실행될 핸들러 노드를 정의한다.
         *
         * @param id 노드 ID
         * @param handlerName 핸들러 이름
         * @return 빌더
         */
        public Builder sinkHandler(String id, String handlerName) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SINK_HANDLER, handlerName);
            nodes.put(id, node);
            return this;
        }

        /**
         * 종료 노드가 될 Sink 정의를 추가한다.
         *
         * @param id 노드 ID
         * @param sinkType Sink 타입 이름
         * @return 빌더
         */
        public Builder sink(String id, String sinkType) {
            WorkflowNodeDefinition node =
                new WorkflowNodeDefinition(id, WorkflowNodeKind.SINK, sinkType);
            nodes.put(id, node);
            endNodeIds.add(id);
            return this;
        }

        /**
         * 워크플로 실행 옵션(배치/백프레셔 등)을 설정한다.
         *
         * @param batchingOptions 실행 옵션
         * @return 빌더
         */
        public Builder batchingOptions(com.ees.framework.workflow.engine.WorkflowEngine.BatchingOptions batchingOptions) {
            this.batchingOptions = batchingOptions;
            return this;
        }

        /**
         * 노드 간 단순 연결(조건 없음)을 정의한다.
         *
         * @param fromId 출발 노드 ID
         * @param toId 도착 노드 ID
         * @return 빌더
         */
        public Builder edge(String fromId, String toId) {
            return edge(fromId, toId, null);
        }

        /**
         * 조건(optional)을 포함한 엣지 연결을 정의한다.
         *
         * @param fromId 출발 노드 ID
         * @param toId 도착 노드 ID
         * @param condition 조건(없으면 null)
         * @return 빌더
         */
        public Builder edge(String fromId, String toId, String condition) {
            WorkflowEdgeDefinition edge = new WorkflowEdgeDefinition(fromId, toId, condition);
            edges.add(edge);
            endNodeIds.remove(fromId);
            return this;
        }

        /**
         * 필수 요소를 검증하고 그래프 정의를 생성한다.
         *
         * @return 완성된 WorkflowGraphDefinition
         * @throws IllegalStateException 시작/종료 노드가 유효하게 구성되지 않은 경우
         */
        public WorkflowGraphDefinition build() {
            if (startNodeId == null) {
                throw new IllegalStateException("startNodeId is not set (no source node?)");
            }
            if (endNodeIds.isEmpty()) {
                inferEndNodesFromSinks();
            }
            return new WorkflowGraphDefinition(
                name,
                startNodeId,
                endNodeIds,
                new ArrayList<>(nodes.values()),
                edges,
                batchingOptions
            );
        }

        /**
         * 명시적 종료 노드가 없을 경우 SINK 노드를 종료로 간주한다.
         */
        private void inferEndNodesFromSinks() {
            for (WorkflowNodeDefinition node : nodes.values()) {
                if (node.getKind() == WorkflowNodeKind.SINK) {
                    endNodeIds.add(node.getId());
                }
            }
        }
    }
}
