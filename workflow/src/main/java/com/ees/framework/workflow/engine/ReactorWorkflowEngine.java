package com.ees.framework.workflow.engine;

import com.ees.framework.context.FxContext;
import com.ees.framework.handlers.SinkHandler;
import com.ees.framework.handlers.SourceHandler;
import com.ees.framework.pipeline.PipelineStep;
import com.ees.framework.sink.Sink;
import com.ees.framework.source.Source;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.model.WorkflowNodeKind;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WorkflowGraphDefinition + WorkflowNodeResolver 를 이용해
 * Reactor 기반 Workflow 구현을 생성하는 엔진 스켈레톤.
 *
 * 실제 그래프 -> Flux 파이프라인 매핑 로직은
 * 이후 단계에서 이 클래스 안에 채워 넣는다.
 */
@Slf4j
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
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class DefaultWorkflow implements Workflow {

        private final WorkflowGraphDefinition graph;
        private final WorkflowNodeResolver resolver;

        // 필요시 Reactor 구독 핸들(Disposable 등)을 여기에 보관
        private Disposable subscription;

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
            log.info("Starting workflow: {}", graph.getName());

            Map<String, WorkflowNodeDefinition> nodesById = graph.getNodes().stream()
                .collect(Collectors.toMap(WorkflowNodeDefinition::getId, n -> n));
            Map<String, List<String>> edgesByFrom = graph.getEdges().stream()
                .collect(Collectors.groupingBy(
                    edge -> edge.getFromNodeId(),
                    Collectors.mapping(edge -> edge.getToNodeId(), Collectors.toList())
                ));

            WorkflowNodeDefinition startNode = findNode(graph.getStartNodeId());
            if (startNode.getKind() != WorkflowNodeKind.SOURCE) {
                return Mono.error(new IllegalStateException("Start node must be SOURCE: " + startNode.getId()));
            }

            @SuppressWarnings("unchecked")
            Source<Object> source = (Source<Object>) resolver.resolve(startNode);
            Flux<FxContext<Object>> flux = source.read();

            WorkflowNodeDefinition current = startNode;
            while (true) {
                List<WorkflowNodeDefinition> nextNodes = successors(current, nodesById, edgesByFrom);
                if (nextNodes.isEmpty()) {
                    break;
                }
                if (nextNodes.size() > 1) {
                    return Mono.error(new IllegalStateException(
                        "Branching workflows are not supported yet for node: " + current.getId()));
                }
                WorkflowNodeDefinition next = nextNodes.get(0);

                switch (next.getKind()) {
                    case SOURCE_HANDLER -> {
                        @SuppressWarnings("unchecked")
                        SourceHandler<Object> handler = (SourceHandler<Object>) resolver.resolve(next);
                        flux = applySourceHandler(flux, handler);
                    }
                    case PIPELINE_STEP -> {
                        @SuppressWarnings("unchecked")
                        PipelineStep<Object, Object> step = (PipelineStep<Object, Object>) resolver.resolve(next);
                        flux = applyPipelineStep(flux, step);
                    }
                    case SINK_HANDLER -> {
                        @SuppressWarnings("unchecked")
                        SinkHandler<Object> handler = (SinkHandler<Object>) resolver.resolve(next);
                        flux = applySinkHandler(flux, handler);
                    }
                    case SINK -> {
                        @SuppressWarnings("unchecked")
                        Sink<Object> sink = (Sink<Object>) resolver.resolve(next);
                        Mono<Void> run = flux.flatMap(ctx -> sink.write(ctx)).then();
                        this.subscription = run.subscribe();
                        return Mono.empty();
                    }
                    case SOURCE -> throw new IllegalStateException("Unexpected SOURCE after start: " + next.getId());
                }

                current = next;
            }

            // Sink 노드가 없으면 단순히 완료 시그널 반환
            this.subscription = flux.then().subscribe();
            return Mono.empty();
        }

        /**
         * 실행 중인 워크플로우를 중지하고 리소스를 정리한다.
         *
         * @return 완료 신호(Mono) - 실제 구현에서는 구독 해제 후 반환 예정
         */
        @Override
        public Mono<Void> stop() {
            log.info("Stopping workflow: {}", graph.getName());
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
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

        private List<WorkflowNodeDefinition> successors(
            WorkflowNodeDefinition node,
            Map<String, WorkflowNodeDefinition> nodesById,
            Map<String, List<String>> edgesByFrom
        ) {
            List<String> toIds = edgesByFrom.getOrDefault(node.getId(), List.of());
            List<WorkflowNodeDefinition> next = new ArrayList<>();
            for (String id : toIds) {
                WorkflowNodeDefinition target = nodesById.get(id);
                if (target != null) {
                    next.add(target);
                }
            }
            return next;
        }

        private Flux<FxContext<Object>> applySourceHandler(
            Flux<FxContext<Object>> flux,
            SourceHandler<Object> handler
        ) {
            return flux.flatMap(ctx ->
                handler.supports(ctx)
                    ? handler.handle(ctx)
                    : Mono.just(ctx)
            );
        }

        private Flux<FxContext<Object>> applySinkHandler(
            Flux<FxContext<Object>> flux,
            SinkHandler<Object> handler
        ) {
            return flux.flatMap(ctx ->
                handler.supports(ctx)
                    ? handler.handle(ctx)
                    : Mono.just(ctx)
            );
        }

        private Flux<FxContext<Object>> applyPipelineStep(
            Flux<FxContext<Object>> flux,
            PipelineStep<Object, Object> step
        ) {
            return flux.flatMap(ctx ->
                step.supports(ctx)
                    ? step.apply(ctx)
                    : Mono.just(ctx)
            );
        }

        @SuppressWarnings("unused")
        private void debugPrintGraph() {
            log.info("WorkflowGraphDefinition: {}", graph.getName());
            graph.getNodes().forEach(node -> {
                log.info("  Node: {} kind={} ref={}", node.getId(), node.getKind(), node.getRefName());
            });
            graph.getEdges().forEach(edge -> {
                log.info("  Edge: {} -> {} cond={}", edge.getFromNodeId(), edge.getToNodeId(), edge.getCondition());
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
