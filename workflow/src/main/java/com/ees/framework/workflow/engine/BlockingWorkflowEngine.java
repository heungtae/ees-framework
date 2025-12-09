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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * WorkflowGraphDefinition + WorkflowNodeResolver 를 이용해
 * Reactor 기반 Workflow 구현을 생성하는 엔진 스켈레톤.
 *
 * 실제 그래프 -> Flux 파이프라인 매핑 로직은
 * 이후 단계에서 이 클래스 안에 채워 넣는다.
 */
@Slf4j
public class BlockingWorkflowEngine {

    private final BatchingOptions batching;

    public BlockingWorkflowEngine() {
        this(BatchingOptions.defaults());
    }

    public BlockingWorkflowEngine(BatchingOptions batching) {
        this.batching = Objects.requireNonNull(batching, "batching must not be null");
    }

    /**
     * 주어진 WorkflowGraphDefinition 을 실행 가능한 Workflow 로 변환한다.
     *
     * @param graph    그래프 기반 워크플로우 정의
     * @param resolver 노드 -> 실제 Bean/구현체 resolve 담당
     */
    public Workflow createWorkflow(WorkflowGraphDefinition graph, WorkflowNodeResolver resolver) {
        return new DefaultWorkflow(graph, resolver, batching);
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
        private final BatchingOptions batching;

        private volatile boolean running;

        @Override
        public String getName() {
            return graph.getName();
        }

        /**
         * 그래프 정의를 기반으로 파이프라인을 조립하고 실행을 시작한다.
         */
        @Override
        public void start() {
            log.info("Starting workflow: {}", graph.getName());
            running = true;

            Map<String, WorkflowNodeDefinition> nodesById = graph.getNodes().stream()
                .collect(Collectors.toMap(WorkflowNodeDefinition::getId, n -> n));
            Map<String, List<String>> edgesByFrom = graph.getEdges().stream()
                .collect(Collectors.groupingBy(
                    edge -> edge.getFromNodeId(),
                    Collectors.mapping(edge -> edge.getToNodeId(), Collectors.toList())
                ));

            WorkflowNodeDefinition startNode = findNode(graph.getStartNodeId());
            if (startNode.getKind() != WorkflowNodeKind.SOURCE) {
                throw new IllegalStateException("Start node must be SOURCE: " + startNode.getId());
            }

            @SuppressWarnings("unchecked")
            Source<Object> source = (Source<Object>) resolver.resolve(startNode);
            boolean continuous = batching.continuous();
            do {
                Iterable<FxContext<Object>> iterable = source.read();
                boolean processed = false;

                WorkflowNodeDefinition current = startNode;
                while (running) {
                    List<WorkflowNodeDefinition> nextNodes = successors(current, nodesById, edgesByFrom);
                    if (nextNodes.isEmpty()) {
                        break;
                    }
                    if (nextNodes.size() > 1) {
                        throw new IllegalStateException(
                            "Branching workflows are not supported yet for node: " + current.getId());
                    }
                    WorkflowNodeDefinition next = nextNodes.get(0);

                    switch (next.getKind()) {
                        case SOURCE_HANDLER -> {
                            @SuppressWarnings("unchecked")
                            SourceHandler<Object> handler = (SourceHandler<Object>) resolver.resolve(next);
                            iterable = applySourceHandler(iterable, handler);
                        }
                        case PIPELINE_STEP -> {
                            @SuppressWarnings("unchecked")
                            PipelineStep<Object, Object> step = (PipelineStep<Object, Object>) resolver.resolve(next);
                            iterable = applyPipelineStep(iterable, step);
                        }
                        case SINK_HANDLER -> {
                            @SuppressWarnings("unchecked")
                            SinkHandler<Object> handler = (SinkHandler<Object>) resolver.resolve(next);
                            iterable = applySinkHandler(iterable, handler);
                        }
                        case SINK -> {
                            @SuppressWarnings("unchecked")
                            Sink<Object> sink = (Sink<Object>) resolver.resolve(next);
                            processed = writeWithBatching(iterable, sink) || processed;
                            break;
                        }
                        case SOURCE -> throw new IllegalStateException("Unexpected SOURCE after start: " + next.getId());
                    }

                    if (next.getKind() == WorkflowNodeKind.SINK) {
                        break;
                    }
                    current = next;
                }

                if (!processed && running) {
                    sleepQuietly(batching.batchTimeout());
                }
            } while (running && continuous);
        }

        /**
         * 실행 중인 워크플로우를 중지하고 리소스를 정리한다.
         */
        @Override
        public void stop() {
            log.info("Stopping workflow: {}", graph.getName());
            running = false;
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

        private Iterable<FxContext<Object>> applySourceHandler(
            Iterable<FxContext<Object>> iterable,
            SourceHandler<Object> handler
        ) {
            return mapIterable(iterable, ctx -> handler.supports(ctx) ? handler.handle(ctx) : ctx);
        }

        private Iterable<FxContext<Object>> applySinkHandler(
            Iterable<FxContext<Object>> iterable,
            SinkHandler<Object> handler
        ) {
            return mapIterable(iterable, ctx -> handler.supports(ctx) ? handler.handle(ctx) : ctx);
        }

        private Iterable<FxContext<Object>> applyPipelineStep(
            Iterable<FxContext<Object>> iterable,
            PipelineStep<Object, Object> step
        ) {
            return mapIterable(iterable, ctx -> step.supports(ctx) ? step.apply(ctx) : ctx);
        }

        private Iterable<FxContext<Object>> mapIterable(
            Iterable<FxContext<Object>> iterable,
            java.util.function.Function<FxContext<Object>, FxContext<Object>> mapper
        ) {
            return () -> new java.util.Iterator<>() {
                private final java.util.Iterator<FxContext<Object>> delegate = iterable.iterator();

                @Override
                public boolean hasNext() {
                    return running && delegate.hasNext();
                }

                @Override
                public FxContext<Object> next() {
                    return mapper.apply(delegate.next());
                }
            };
        }

        private boolean writeWithBatching(Iterable<FxContext<Object>> iterable, Sink<Object> sink) {
            ArrayBlockingQueue<FxContext<Object>> queue = new ArrayBlockingQueue<>(batching.queueCapacity());
            AtomicBoolean producing = new AtomicBoolean(true);
            AtomicBoolean draining = new AtomicBoolean(true);
            ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
            Future<?> drainFuture = executor.submit(() -> drain(queue, sink, producing, draining));
            long sent = 0L;
            try {
                Duration offerTimeout = batching.batchTimeout();
                for (FxContext<Object> ctx : iterable) {
                    if (!running) {
                        break;
                    }
                    boolean enqueued = queue.offer(ctx, offerTimeout.toMillis(), TimeUnit.MILLISECONDS);
                    if (!enqueued) {
                        throw new IllegalStateException("Workflow queue is full; backpressure threshold exceeded");
                    }
                    sent++;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while enqueuing workflow items", ex);
            } finally {
                producing.set(false);
                draining.set(false);
                try {
                    drainFuture.get(batching.batchTimeout().toMillis() * 2, TimeUnit.MILLISECONDS);
                } catch (Exception ex) {
                    drainFuture.cancel(true);
                    throw new IllegalStateException("Failed draining workflow queue", ex);
                } finally {
                    executor.shutdownNow();
                }
            }

            return sent > 0;
        }

        private void drain(ArrayBlockingQueue<FxContext<Object>> queue, Sink<Object> sink, AtomicBoolean producing, AtomicBoolean draining) {
            List<FxContext<Object>> batch = new ArrayList<>(batching.batchSize());
            try {
                while (draining.get() || producing.get() || !queue.isEmpty()) {
                    FxContext<Object> first = queue.poll(batching.batchTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    if (first == null) {
                        continue;
                    }
                    batch.add(first);
                    queue.drainTo(batch, batching.batchSize() - 1);
                    writeBatch(batch, sink);
                    batch.clear();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (!batch.isEmpty()) {
                    writeBatch(batch, sink);
                    batch.clear();
                }
                FxContext<Object> remaining;
                while ((remaining = queue.poll()) != null) {
                    writeBatch(List.of(remaining), sink);
                }
            }
        }

        private void writeBatch(List<FxContext<Object>> batch, Sink<Object> sink) {
            for (FxContext<Object> ctx : batch) {
                sink.write(ctx);
            }
        }

        private void sleepQuietly(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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

    /**
     * 배치 및 백프레셔 옵션.
     */
    public record BatchingOptions(int queueCapacity, int batchSize, Duration batchTimeout, boolean continuous) {
        public BatchingOptions {
            if (queueCapacity <= 0) {
                throw new IllegalArgumentException("queueCapacity must be > 0");
            }
            if (batchSize <= 0) {
                throw new IllegalArgumentException("batchSize must be > 0");
            }
            Objects.requireNonNull(batchTimeout, "batchTimeout must not be null");
            if (batchTimeout.isNegative() || batchTimeout.isZero()) {
                throw new IllegalArgumentException("batchTimeout must be > 0");
            }
        }

        public static BatchingOptions defaults() {
            return new BatchingOptions(256, 32, Duration.ofMillis(200), false);
        }
    }
}
