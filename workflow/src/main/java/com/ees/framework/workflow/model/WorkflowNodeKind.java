package com.ees.framework.workflow.model;

/**
 * Workflow 그래프 내에서 노드 종류.
 */
public enum WorkflowNodeKind {
    /** 실제 데이터 소스 */
    SOURCE,
    /** 소스 데이터에 적용되는 핸들러 */
    SOURCE_HANDLER,
    /** 파이프라인 변환 단계 */
    PIPELINE_STEP,
    /** 싱크 전달 전/후 핸들러 */
    SINK_HANDLER,
    /** 출력 대상 */
    SINK
}
