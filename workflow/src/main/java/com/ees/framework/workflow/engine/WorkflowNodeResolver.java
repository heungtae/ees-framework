package com.ees.framework.workflow.engine;

import com.ees.framework.workflow.model.WorkflowNodeDefinition;

/**
 * WorkflowNodeDefinition 의 refName/kind 등을 이용해
 * 실제 Spring Bean 또는 구현체 인스턴스를 찾아주는 역할.
 *
 * 예:
 *  - SOURCE 노드  -> Source Bean
 *  - PIPELINE_STEP 노드 -> PipelineStep Bean
 *  - SINK_HANDLER 노드 -> SinkHandler Bean
 *  - SINK 노드 -> Sink Bean
 */
public interface WorkflowNodeResolver {

    /**
     * 주어진 노드를 해석해 실제 실행 객체로 변환한다.
     *
     * 반환 타입은 구현마다 다르므로 Object 로 두고,
     * ReactorWorkflowEngine 에서 kind 에 따라 캐스팅하여 사용한다.
     *
     * @param node 워크플로우 노드 정의
     * @return 실제 실행 객체 (Source, Handler, PipelineStep, Sink 등)
     */
    Object resolve(WorkflowNodeDefinition node);
}
