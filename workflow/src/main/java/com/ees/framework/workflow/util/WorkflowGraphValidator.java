package com.ees.framework.workflow.util;

import com.ees.framework.workflow.model.WorkflowEdgeDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;

import java.util.*;

/**
 * WorkflowGraphDefinition 을 검증하는 유틸리티.
 * - DAG(사이클 없음) 검증 (간단한 Kahn 알고리즘)
 * - start / end 노드 기본 체크
 */
public class WorkflowGraphValidator {

    /**
     * 워크플로 그래프 정의의 기본 무결성을 검증한다.
     *
     * @param graph 검증 대상 그래프 정의
     * @throws NullPointerException     그래프가 null 인 경우
     * @throws IllegalStateException    시작/종료 노드가 누락되었거나 엣지가 유효하지 않은 경우
     */
    public void validate(WorkflowGraphDefinition graph) {
        Objects.requireNonNull(graph, "graph must not be null");
        ensureStartNodeExists(graph);
        ensureEndNodesExist(graph);
        ensureDag(graph);
    }

    private void ensureStartNodeExists(WorkflowGraphDefinition graph) {
        String startId = graph.getStartNodeId();
        boolean exists = graph.getNodes().stream()
            .anyMatch(n -> n.getId().equals(startId));
        if (!exists) {
            throw new IllegalStateException("Start node not found: " + startId);
        }
    }

    private void ensureEndNodesExist(WorkflowGraphDefinition graph) {
        Set<String> endIds = graph.getEndNodeIds();
        if (endIds.isEmpty()) {
            throw new IllegalStateException("No end nodes defined in graph: " + graph.getName());
        }
        Set<String> nodeIds = new HashSet<>();
        for (WorkflowNodeDefinition n : graph.getNodes()) {
            nodeIds.add(n.getId());
        }
        for (String endId : endIds) {
            if (!nodeIds.contains(endId)) {
                throw new IllegalStateException("End node not found: " + endId);
            }
        }
    }

    private void ensureDag(WorkflowGraphDefinition graph) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();

        for (WorkflowNodeDefinition node : graph.getNodes()) {
            inDegree.put(node.getId(), 0);
            outgoing.put(node.getId(), new ArrayList<>());
        }

        for (WorkflowEdgeDefinition edge : graph.getEdges()) {
            String from = edge.getFromNodeId();
            String to = edge.getToNodeId();
            if (!inDegree.containsKey(from) || !inDegree.containsKey(to)) {
                throw new IllegalStateException("Edge references unknown node: " + from + " -> " + to);
            }
            outgoing.get(from).add(to);
            inDegree.put(to, inDegree.get(to) + 1);
        }

        Deque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        int visitCount = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            visitCount++;
            for (String next : outgoing.get(current)) {
                int deg = inDegree.get(next) - 1;
                inDegree.put(next, deg);
                if (deg == 0) {
                    queue.add(next);
                }
            }
        }

        if (visitCount < inDegree.size()) {
            throw new IllegalStateException("Workflow graph contains a cycle: " + graph.getName());
        }
    }
}
