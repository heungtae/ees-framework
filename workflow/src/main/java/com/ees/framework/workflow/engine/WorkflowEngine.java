package com.ees.framework.workflow.engine;

import com.ees.framework.context.FxContext;
import com.ees.framework.handlers.SinkHandler;
import com.ees.framework.handlers.SourceHandler;
import com.ees.framework.pipeline.PipelineStep;
import com.ees.framework.sink.Sink;
import com.ees.framework.source.Source;
import com.ees.framework.context.FxAffinity;
import com.ees.framework.workflow.affinity.AffinityKeyResolver;
import com.ees.framework.workflow.affinity.DefaultAffinityKeyResolver;
import com.ees.framework.workflow.model.WorkflowEdgeDefinition;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * WorkflowGraphDefinition과 WorkflowNodeResolver를 기반으로 키 단위 순서 보장을 유지하며 워크플로우를 실행하는 엔진.
 * Source → Handler → Step → Sink 체인을 조립하고 affinity별 워커를 통해 병렬 처리한다.
 */
@Slf4j
public class WorkflowEngine {

    private final BatchingOptions batching;
    private final AffinityKeyResolver affinityKeyResolver;

    /**
     * 기본 배치 옵션과 DefaultAffinityKeyResolver 로 워크플로 엔진을 생성한다.
     */
    public WorkflowEngine() {
        this(BatchingOptions.defaults(), new DefaultAffinityKeyResolver());
    }

    /**
     * 주어진 배치 옵션과 기본 affinity 리졸버로 워크플로 엔진을 생성한다.
     *
     * @param batching 배치/백프레셔 옵션
     */
    public WorkflowEngine(BatchingOptions batching) {
        this(batching, new DefaultAffinityKeyResolver());
    }

    /**
     * 배치 옵션과 affinity 리졸버를 모두 주입받아 워크플로 엔진을 생성한다.
     *
     * @param batching 배치/백프레셔 옵션
     * @param affinityKeyResolver affinity kind/value 를 계산할 리졸버
     */
    public WorkflowEngine(BatchingOptions batching, AffinityKeyResolver affinityKeyResolver) {
        this.batching = Objects.requireNonNull(batching, "batching must not be null");
        this.affinityKeyResolver = Objects.requireNonNull(affinityKeyResolver, "affinityKeyResolver must not be null");
        log.info("Initialized WorkflowEngine batchingOptions={} affinityKeyResolver={}",
            this.batching, this.affinityKeyResolver.getClass().getSimpleName());
        if (log.isDebugEnabled() && this.affinityKeyResolver instanceof DefaultAffinityKeyResolver resolver) {
            log.debug("WorkflowEngine defaultAffinityKind={}", resolver.defaultKind());
        }
    }

    /**
     * 주어진 WorkflowGraphDefinition 을 실행 가능한 Workflow 로 변환한다.
     *
     * @param graph    그래프 기반 워크플로우 정의
     * @param resolver 노드 -> 실제 Bean/구현체 resolve 담당
     * @return 실행 가능한 Workflow 인스턴스
     */
    public Workflow createWorkflow(WorkflowGraphDefinition graph, WorkflowNodeResolver resolver) {
        BatchingOptions effectiveBatching = resolveBatching(graph);
        if (log.isDebugEnabled()) {
            log.debug("Creating workflow name={} nodes={} edges={} batchingOptions={}",
                graph.getName(), graph.getNodes().size(), graph.getEdges().size(), effectiveBatching);
        }
        return new DefaultWorkflow(graph, resolver, effectiveBatching, affinityKeyResolver);
    }
    // resolveBatching 동작을 수행한다.

    private BatchingOptions resolveBatching(WorkflowGraphDefinition graph) {
        if (graph == null || graph.getBatchingOptions() == null) {
            return batching;
        }
        return graph.getBatchingOptions();
    }

    /**
     * 클러스터 토폴로지 변경 등에 맞춰 기본 affinity kind 를 갱신한다.
     *
     * @param kind 새 기본 affinity kind
     */
    public void updateAffinityKind(String kind) {
        if (this.affinityKeyResolver instanceof DefaultAffinityKeyResolver mutable) {
            mutable.setDefaultKind(kind);
            log.info("Updated workflow affinity kind to {}", kind);
        } else {
            log.warn("Cannot update affinity kind because resolver {} is not mutable", affinityKeyResolver.getClass().getSimpleName());
        }
    }

    /**
     * 기본 Workflow 구현.
     * 현재는 스켈레톤으로 start/stop 에서 간단한 로그만 출력하고,
     * 실제 Reactor 파이프라인은 TODO 로 남겨둔다.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private class DefaultWorkflow implements Workflow {

        private final WorkflowGraphDefinition graph;
        private final WorkflowNodeResolver resolver;
        private final BatchingOptions batching;
        private final AffinityKeyResolver affinityKeyResolver;
        // AtomicBoolean 동작을 수행한다.

        private final AtomicBoolean running = new AtomicBoolean(false);
        // AtomicBoolean 동작을 수행한다.
        private final AtomicBoolean accepting = new AtomicBoolean(false);
        private final ExecutorService workerExecutor =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        private final ConcurrentMap<FxAffinity, PerKeyWorker> workers = new ConcurrentHashMap<>();
        /**
         * name를 반환한다.
         * @return 
         */

        @Override
        public String getName() {
            return graph.getName();
        }

        /**
         * 그래프 정의를 기반으로 파이프라인을 조립하고 per-key 워커를 구동한다.
         */
        @Override
        public void start() {
            if (!running.compareAndSet(false, true)) {
                log.warn("Workflow {} is already running", graph.getName());
                return;
            }
            log.info("Starting workflow: {}", graph.getName());
            if (log.isDebugEnabled()) {
                log.debug("Workflow {} batchingOptions={} startNodeId={}",
                    graph.getName(), batching, graph.getStartNodeId());
            }

            WorkflowNodeDefinition startNode = findNode(graph.getStartNodeId());
            if (startNode.getKind() != WorkflowNodeKind.SOURCE) {
                throw new IllegalStateException("Start node must be SOURCE: " + startNode.getId());
            }
            PipelineChain chain = buildPipelineChain(startNode);
            if (log.isDebugEnabled()) {
                log.debug("Workflow {} pipeline processors={} sink={}",
                    graph.getName(),
                    chain.processors().size(),
                    chain.sink().getClass().getSimpleName());
            }
            @SuppressWarnings("unchecked")
            Source<Object> source = (Source<Object>) resolver.resolve(startNode);

            boolean continuous = batching.continuous();
            do {
                accepting.set(true);
                long dispatched = dispatch(source, chain);
                accepting.set(false);
                waitForPendingWork();
                if (dispatched == 0 && running.get() && continuous) {
                    sleepQuietly(batching.batchTimeout());
                }
            } while (running.get() && continuous);

            waitForPendingWork();
        }

        /**
         * 실행 중인 워크플로우를 중지하고 리소스를 정리한다.
         */
        @Override
        public void stop() {
            if (!running.compareAndSet(true, false)) {
                return;
            }
            accepting.set(false);
            log.info("Stopping workflow: {}", graph.getName());
            workers.values().forEach(PerKeyWorker::stop);
            waitForPendingWork();
            workerExecutor.shutdown();
        }
        // dispatch 동작을 수행한다.

        private long dispatch(Source<Object> source, PipelineChain chain) {
            long count = 0L;
            for (FxContext<Object> ctx : source.read()) {
                if (!running.get()) {
                    break;
                }
                FxContext<Object> normalized = normalizeAffinity(ctx);
                PerKeyWorker worker = workers.computeIfAbsent(normalized.affinity(), key -> createWorker(key, chain));
                worker.enqueue(normalized);
                count++;
            }
            return count;
        }
        // createWorker 동작을 수행한다.

        private PerKeyWorker createWorker(FxAffinity affinity, PipelineChain chain) {
            PerKeyWorker worker = new PerKeyWorker(affinity, chain);
            worker.start();
            return worker;
        }
        // buildPipelineChain 동작을 수행한다.

        private PipelineChain buildPipelineChain(WorkflowNodeDefinition startNode) {
            Map<String, WorkflowNodeDefinition> nodesById = graph.getNodes().stream()
                .collect(Collectors.toMap(WorkflowNodeDefinition::getId, n -> n));
            Map<String, List<String>> edgesByFrom = graph.getEdges().stream()
                .collect(Collectors.groupingBy(
                    edge -> edge.getFromNodeId(),
                    Collectors.mapping(WorkflowEdgeDefinition::getToNodeId, Collectors.toList())
                ));

            List<java.util.function.Function<FxContext<Object>, FxContext<Object>>> processors = new ArrayList<>();
            Sink<Object> sink = null;
            WorkflowNodeDefinition current = startNode;
            while (true) {
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
                        processors.add(sourceHandlerFn(handler));
                    }
                    case PIPELINE_STEP -> {
                        @SuppressWarnings("unchecked")
                        PipelineStep<Object, Object> step = (PipelineStep<Object, Object>) resolver.resolve(next);
                        processors.add(pipelineStepFn(step));
                    }
                    case SINK_HANDLER -> {
                        @SuppressWarnings("unchecked")
                        SinkHandler<Object> handler = (SinkHandler<Object>) resolver.resolve(next);
                        processors.add(sinkHandlerFn(handler));
                    }
                    case SINK -> {
                        @SuppressWarnings("unchecked")
                        Sink<Object> resolvedSink = (Sink<Object>) resolver.resolve(next);
                        sink = resolvedSink;
                    }
                    case SOURCE -> throw new IllegalStateException("Unexpected SOURCE after start: " + next.getId());
                }
                if (next.getKind() == WorkflowNodeKind.SINK) {
                    break;
                }
                current = next;
            }
            if (sink == null) {
                throw new IllegalStateException("Sink node is required for workflow: " + graph.getName());
            }
            return new PipelineChain(processors, sink);
        }
        // successors 동작을 수행한다.

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
        // sourceHandlerFn 동작을 수행한다.

        private java.util.function.Function<FxContext<Object>, FxContext<Object>> sourceHandlerFn(
            SourceHandler<Object> handler
        ) {
            return ctx -> {
                FxContext<Object> normalized = normalizeAffinity(ctx);
                return handler.supports(normalized) ? handler.handle(normalized) : normalized;
            };
        }
        // sinkHandlerFn 동작을 수행한다.

        private java.util.function.Function<FxContext<Object>, FxContext<Object>> sinkHandlerFn(
            SinkHandler<Object> handler
        ) {
            return ctx -> {
                FxContext<Object> normalized = normalizeAffinity(ctx);
                return handler.supports(normalized) ? handler.handle(normalized) : normalized;
            };
        }
        // pipelineStepFn 동작을 수행한다.

        private java.util.function.Function<FxContext<Object>, FxContext<Object>> pipelineStepFn(
            PipelineStep<Object, Object> step
        ) {
            return ctx -> {
                FxContext<Object> normalized = normalizeAffinity(ctx);
                return step.supports(normalized) ? step.apply(normalized) : normalized;
            };
        }
        // waitForPendingWork 동작을 수행한다.

        private void waitForPendingWork() {
            Duration waitWindow = batching.cleanupIdleAfter().plus(batching.batchTimeout());
            long deadlineNanos = System.nanoTime() + waitWindow.toNanos();
            while (hasPendingWork() && System.nanoTime() < deadlineNanos) {
                sleepQuietly(Duration.ofMillis(10));
            }
        }
        // hasPendingWork 동작을 수행한다.

        private boolean hasPendingWork() {
            return workers.values().stream().anyMatch(PerKeyWorker::hasPendingWork);
        }
        // normalizeAffinity 동작을 수행한다.

        private FxContext<Object> normalizeAffinity(FxContext<Object> context) {
            FxAffinity resolved = affinityKeyResolver.resolve(context);
            if (resolved == null || resolved.value() == null) {
                throw new IllegalStateException("Affinity value is required for ordered/parallel execution");
            }
            if (resolved.kind() == null) {
                throw new IllegalStateException("Affinity kind is required for ordered/parallel execution");
            }
            if (affinityKeyResolver instanceof DefaultAffinityKeyResolver resolver) {
                String expectedKind = resolver.defaultKind();
                if (expectedKind != null && !expectedKind.equals(resolved.kind())) {
                    String message = "Affinity kind mismatch: expected %s but got %s".formatted(expectedKind, resolved.kind());
                    log.error(message);
                    throw new IllegalStateException(message);
                }
            }
            return context.withAffinity(resolved);
        }
        // sleepQuietly 동작을 수행한다.

        private void sleepQuietly(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // removeWorker 동작을 수행한다.

        private PerKeyWorker removeWorker(FxAffinity affinity) {
            return workers.remove(affinity);
        }

        /**
         * Per-key mailbox + worker that drains contexts in order and applies the pipeline chain.
         */
        private final class PerKeyWorker implements Runnable {

            private final FxAffinity affinity;
            private final PipelineChain chain;
            // queueCapacity 동작을 수행한다.
            private final ArrayBlockingQueue<FxContext<Object>> queue = new ArrayBlockingQueue<>(batching.queueCapacity());
            // AtomicBoolean 동작을 수행한다.
            private final AtomicBoolean active = new AtomicBoolean(true);
            // AtomicBoolean 동작을 수행한다.
            private final AtomicBoolean processing = new AtomicBoolean(false);
            // nanoTime 동작을 수행한다.
            private volatile long lastActivityNanos = System.nanoTime();
            private Future<?> task;
            // PerKeyWorker 동작을 수행한다.

            private PerKeyWorker(FxAffinity affinity, PipelineChain chain) {
                this.affinity = affinity;
                this.chain = chain;
            }
            // start 동작을 수행한다.

            private void start() {
                task = workerExecutor.submit(this);
                if (log.isDebugEnabled()) {
                    log.debug("Started per-key worker workflow={} affinity={}", graph.getName(), affinity);
                }
            }
            // stop 동작을 수행한다.

            private void stop() {
                active.set(false);
                if (task != null && !task.isDone() && !task.isCancelled()) {
                    task.cancel(true);
                }
            }
            // enqueue 동작을 수행한다.

            private void enqueue(FxContext<Object> context) {
                if (!running.get()) {
                    throw new IllegalStateException("Workflow is stopping; cannot enqueue new context");
                }
                try {
                    switch (batching.backpressurePolicy()) {
                        case BLOCK -> {
                            boolean enqueued = queue.offer(context, batching.batchTimeout().toMillis(), TimeUnit.MILLISECONDS);
                            if (!enqueued) {
                                throw new IllegalStateException("Workflow queue is full; backpressure threshold exceeded");
                            }
                        }
                        case DROP_OLDEST -> {
                            if (!queue.offer(context)) {
                                queue.poll();
                                boolean enqueued = queue.offer(context);
                                if (!enqueued) {
                                    throw new IllegalStateException("Workflow queue is full; backpressure threshold exceeded after drop-oldest");
                                }
                                if (log.isDebugEnabled()) {
                                    log.debug("Dropped oldest item due to backpressure workflow={} affinity={} queueCapacity={}",
                                        graph.getName(), affinity, batching.queueCapacity());
                                }
                            }
                        }
                        case ERROR -> {
                            boolean enqueued = queue.offer(context);
                            if (!enqueued) {
                                throw new IllegalStateException("Workflow queue is full; backpressure threshold exceeded");
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while enqueuing workflow items", ex);
                }
            }
            /**
             * run를 수행한다.
             */

            @Override
            public void run() {
                List<FxContext<Object>> batch = new ArrayList<>(batching.batchSize());
                try {
                    while (shouldContinue()) {
                        FxContext<Object> first = queue.poll(batching.batchTimeout().toMillis(), TimeUnit.MILLISECONDS);
                        if (first == null) {
                            if (shouldCleanup()) {
                                break;
                            }
                            continue;
                        }
                        batch.add(first);
                        queue.drainTo(batch, batching.batchSize() - 1);
                        processBatch(batch);
                        batch.clear();
                        lastActivityNanos = System.nanoTime();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    drainRemaining(batch);
                    removeWorker(affinity);
                    if (log.isDebugEnabled()) {
                        log.debug("Stopped per-key worker workflow={} affinity={}", graph.getName(), affinity);
                    }
                }
            }
            // shouldContinue 동작을 수행한다.

            private boolean shouldContinue() {
                return (running.get() && active.get()) || accepting.get() || !queue.isEmpty();
            }
            // shouldCleanup 동작을 수행한다.

            private boolean shouldCleanup() {
                if (!accepting.get() && queue.isEmpty()) {
                    long idleNanos = System.nanoTime() - lastActivityNanos;
                    return idleNanos >= batching.cleanupIdleAfter().toNanos();
                }
                return false;
            }
            // processBatch 동작을 수행한다.

            private void processBatch(List<FxContext<Object>> batch) {
                processing.set(true);
                try {
                    for (FxContext<Object> ctx : batch) {
                        FxContext<Object> current = ctx;
                        for (java.util.function.Function<FxContext<Object>, FxContext<Object>> processor : chain.processors()) {
                            current = processor.apply(current);
                        }
                        chain.sink().write(current);
                    }
                } finally {
                    processing.set(false);
                }
            }
            // drainRemaining 동작을 수행한다.

            private void drainRemaining(List<FxContext<Object>> reusable) {
                if (!queue.isEmpty()) {
                    queue.drainTo(reusable);
                    if (!reusable.isEmpty()) {
                        processBatch(reusable);
                        reusable.clear();
                    }
                }
                FxContext<Object> remaining;
                while ((remaining = queue.poll()) != null) {
                    processBatch(List.of(remaining));
                }
            }
            // hasPendingWork 동작을 수행한다.

            private boolean hasPendingWork() {
                return processing.get() || !queue.isEmpty();
            }
        }
        // findNode 동작을 수행한다.

        private WorkflowNodeDefinition findNode(String nodeId) {
            return graph.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        }

        private record PipelineChain(
            List<java.util.function.Function<FxContext<Object>, FxContext<Object>>> processors,
            Sink<Object> sink
        ) {
        }
    }

    /**
     * 배치 및 백프레셔 옵션.
     */
    public record BatchingOptions(
        int queueCapacity,
        int batchSize,
        Duration batchTimeout,
        Duration cleanupIdleAfter,
        BackpressurePolicy backpressurePolicy,
        boolean continuous
    ) {
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
            Objects.requireNonNull(cleanupIdleAfter, "cleanupIdleAfter must not be null");
            if (cleanupIdleAfter.isNegative() || cleanupIdleAfter.isZero()) {
                throw new IllegalArgumentException("cleanupIdleAfter must be > 0");
            }
            Objects.requireNonNull(backpressurePolicy, "backpressurePolicy must not be null");
        }

        /**
         * 실행 엔진의 기본 배치/백프레셔 설정을 반환한다.
         *
         * @return 기본 BatchingOptions
         */
        public static BatchingOptions defaults() {
            return new BatchingOptions(
                256,
                32,
                Duration.ofMillis(200),
                Duration.ofSeconds(30),
                BackpressurePolicy.BLOCK,
                false
            );
        }
    }

    public enum BackpressurePolicy {
        BLOCK,
        DROP_OLDEST,
        ERROR
    }
}
