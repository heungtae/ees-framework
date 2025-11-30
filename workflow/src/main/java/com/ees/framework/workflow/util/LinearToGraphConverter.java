package com.ees.framework.workflow.util;

import com.ees.framework.core.ExecutionMode;
import com.ees.framework.workflow.dsl.WorkflowGraphDsl;
import com.ees.framework.workflow.model.HandlerChainDefinition;
import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 선형 WorkflowDefinition 을 그래프 WorkflowGraphDefinition 으로 변환하는 유틸리티.
 *
 * 규칙(기본 버전):
 *  - sourceType -> "source" 노드
 *  - sourceHandlers.mode == SEQUENTIAL:
 *      source -> h1 -> h2 -> ...
 *  - sourceHandlers.mode == PARALLEL:
 *      source -> h1, source -> h2, ...
 *  - pipelineSteps:
 *      lastSourceNode -> step1 -> step2 -> ...
 *  - sinkHandlers.mode == SEQUENTIAL:
 *      lastPipelineNode -> sh1 -> sh2 -> sink
 *  - sinkHandlers.mode == PARALLEL:
 *      lastPipelineNode -> sh1, lastPipelineNode -> sh2, ...
 *      sh1 -> sink, sh2 -> sink, ...
 */
public class LinearToGraphConverter {

    public WorkflowGraphDefinition convert(WorkflowDefinition def) {
        return WorkflowGraphDsl.define(def.getName(), wf -> {
            // 1) source
            wf.source("source", def.getSourceType());
            String lastSourceNodeId = "source";

            // 2) source handlers
            HandlerChainDefinition sh = def.getSourceHandlerChain();
            if (sh != null && !sh.getHandlerNames().isEmpty()) {
                if (sh.getMode() == ExecutionMode.SEQUENTIAL) {
                    lastSourceNodeId = buildSequentialHandlers(
                        wf, "srcH", lastSourceNodeId, sh.getHandlerNames(), true);
                } else {
                    buildParallelHandlers(wf, "srcH", lastSourceNodeId, sh.getHandlerNames(), true);
                }
            }

            // 3) pipeline steps (직선)
            String lastPipelineNodeId = lastSourceNodeId;
            AtomicInteger stepIndex = new AtomicInteger();
            for (String stepName : def.getPipelineSteps()) {
                String id = "pl" + stepIndex.getAndIncrement();
                wf.pipelineStep(id, stepName);
                wf.edge(lastPipelineNodeId, id);
                lastPipelineNodeId = id;
            }

            // 4) sink handlers + sink
            String sinkNodeId = "sink";
            wf.sink(sinkNodeId, def.getSinkType());

            HandlerChainDefinition sinkChain = def.getSinkHandlerChain();
            if (sinkChain != null && !sinkChain.getHandlerNames().isEmpty()) {
                if (sinkChain.getMode() == ExecutionMode.SEQUENTIAL) {
                    // lastPipeline -> sh1 -> sh2 -> sink
                    String last = buildSequentialHandlers(
                        wf, "snkH", lastPipelineNodeId, sinkChain.getHandlerNames(), false);
                    wf.edge(last, sinkNodeId);
                } else {
                    // PARALLEL: lastPipeline -> sh1, lastPipeline -> sh2, ... ; sh* -> sink
                    buildParallelHandlers(
                        wf, "snkH", lastPipelineNodeId, sinkChain.getHandlerNames(), false);
                    int idx = 0;
                    for (int i = 0; i < sinkChain.getHandlerNames().size(); i++) {
                        String id = "snkH" + idx++;
                        wf.edge(id, sinkNodeId);
                    }
                }
            } else {
                // sinkHandlers 없으면 pipeline 마지막에서 바로 sink 연결
                wf.edge(lastPipelineNodeId, sinkNodeId);
            }
        });
    }

    private String buildSequentialHandlers(
        WorkflowGraphDsl.Builder wf,
        String prefix,
        String fromNodeId,
        List<String> handlerNames,
        boolean sourceSide
    ) {
        String lastId = fromNodeId;
        int idx = 0;
        for (String h : handlerNames) {
            String id = prefix + idx++;
            if (sourceSide) {
                wf.sourceHandler(id, h);
            } else {
                wf.sinkHandler(id, h);
            }
            wf.edge(lastId, id);
            lastId = id;
        }
        return lastId;
    }

    private void buildParallelHandlers(
        WorkflowGraphDsl.Builder wf,
        String prefix,
        String fromNodeId,
        List<String> handlerNames,
        boolean sourceSide
    ) {
        int idx = 0;
        for (String h : handlerNames) {
            String id = prefix + idx++;
            if (sourceSide) {
                wf.sourceHandler(id, h);
            } else {
                wf.sinkHandler(id, h);
            }
            wf.edge(fromNodeId, id);
        }
    }
}
