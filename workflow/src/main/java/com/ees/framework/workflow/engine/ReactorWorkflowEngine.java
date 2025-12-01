package com.ees.framework.workflow.engine;

import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.model.WorkflowNodeKind;
import reactor.core.publisher.Mono;

/**
 * WorkflowGraphDefinition + WorkflowNodeResolver 를 이용해
 * Reactor 기반 Workflow 구현을 생성하는 엔진 스켈레톤.
 *
 * 실제 그래프 -> Flux 파이프라인 매핑 로직은
 * 이후 단계에서 이 클래스 안에 채워 넣는다.
 */
public class ReactorWorkflowEngine {

    /**
     * 주어진 WorkflowGraphDefinition 을 실행 가능한 Workflow 로 변환한다.
     *
     * @param graph    그래프 기반 워크플로우 정의
     * @param resolver 노드 -> 실제 Bean/구현체 resolve 담당
     */
    public Workflow createWorkflow(WorkflowGraphDefinition graph, WorkflowNodeResolver resolver) {
        return new DefaultWorkflow(graph, resolver);
    }

    /**
     * 기본 Workflow 구현.
     * 현재는 스켈레톤으로 start/stop 에서 간단한 로그만 출력하고,
     * 실제 Reactor 파이프라인은 TODO 로 남겨둔다.
     */
    private static class DefaultWorkflow implements Workflow {

        private final WorkflowGraphDefinition graph;
        private final WorkflowNodeResolver resolver;

        // 필요시 Reactor 구독 핸들(Disposable 등)을 여기에 보관
        // private Disposable subscription;

        private DefaultWorkflow(WorkflowGraphDefinition graph, WorkflowNodeResolver resolver) {
            this.graph = graph;
            this.resolver = resolver;
        }

        @Override
        public String getName() {
            return graph.getName();
        }

        /**
         * 그래프 정의를 기반으로 파이프라인을 조립하고 실행을 시작한다.
         *
         * @return 완료 신호(Mono) - 실제 구현에서는 Source 구독 완료 후 반환 예정
         */
        @Override
        public Mono<Void> start() {
            // TODO: 여기에 실제 그래프 -> Reactor 파이프라인 조립 로직을 구현.
            //
            // 1. startNodeId 기준으로 Source 노드를 찾고 Source Bean resolve
            // 2. edges 를 따라가며 Handler/Pipeline/Sink 노드들을 Reactor 연산으로 연결
            // 3. Source 의 Flux 를 subscribe 하고, subscription 을 필드에 보관
            //
            //   예시 개념:
            //   WorkflowNodeDefinition startNode = findNode(graph.getStartNodeId());
            //   Object sourceBean = resolver.resolve(startNode);
            //   // Source -> Flux 시작
            //   // 이후 edges 를 따라가며 handler/step/sink 연결...

            System.out.println("[ReactorWorkflowEngine] Starting workflow: " + graph.getName());
            return Mono.empty();
        }

        /**
         * 실행 중인 워크플로우를 중지하고 리소스를 정리한다.
         *
         * @return 완료 신호(Mono) - 실제 구현에서는 구독 해제 후 반환 예정
         */
        @Override
        public Mono<Void> stop() {
            // TODO: start() 에서 유지하던 subscription/리소스 정리 작업 수행.
            System.out.println("[ReactorWorkflowEngine] Stopping workflow: " + graph.getName());
            return Mono.empty();
        }

        // 필요시 helper 메서드들 (현재는 스켈레톤)

        @SuppressWarnings("unused")
        private WorkflowNodeDefinition findNode(String nodeId) {
            return graph.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        }

        @SuppressWarnings("unused")
        private void debugPrintGraph() {
            System.out.println("WorkflowGraphDefinition: " + graph.getName());
            graph.getNodes().forEach(node -> {
                System.out.println("  Node: " + node.getId() +
                    " kind=" + node.getKind() +
                    " ref=" + node.getRefName());
            });
            graph.getEdges().forEach(edge -> {
                System.out.println("  Edge: " + edge.getFromNodeId() + " -> " +
                    edge.getToNodeId() + " cond=" + edge.getCondition());
            });
        }

        @SuppressWarnings("unused")
        private boolean isSourceNode(WorkflowNodeDefinition node) {
            return node.getKind() == WorkflowNodeKind.SOURCE;
        }

        @SuppressWarnings("unused")
        private boolean isSinkNode(WorkflowNodeDefinition node) {
            return node.getKind() == WorkflowNodeKind.SINK;
        }
    }
}
