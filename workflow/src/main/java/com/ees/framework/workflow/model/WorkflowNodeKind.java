package com.ees.framework.workflow.model;

/**
 * Workflow 그래프 내에서 노드 종류.
 */
public enum WorkflowNodeKind {
    SOURCE,
    SOURCE_HANDLER,
    PIPELINE_STEP,
    SINK_HANDLER,
    SINK
}
