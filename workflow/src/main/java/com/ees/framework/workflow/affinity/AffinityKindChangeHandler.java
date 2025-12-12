package com.ees.framework.workflow.affinity;

import com.ees.framework.workflow.engine.WorkflowEngine;
import com.ees.framework.workflow.engine.WorkflowRuntime;

import java.util.Objects;

/**
 * Simple bridge that updates the workflow engine's affinity kind when cluster reports a change.
 * Optionally executes a rebinder (e.g., restart workflows or rebuild per-key workers).
 */
public class AffinityKindChangeHandler {

    private final WorkflowEngine workflowEngine;
    private final Runnable rebinder;

    public AffinityKindChangeHandler(WorkflowEngine workflowEngine) {
        this(workflowEngine, () -> {});
    }

    public AffinityKindChangeHandler(WorkflowEngine workflowEngine, Runnable rebinder) {
        this.workflowEngine = Objects.requireNonNull(workflowEngine, "workflowEngine must not be null");
        this.rebinder = Objects.requireNonNull(rebinder, "rebinder must not be null");
    }

    public static AffinityKindChangeHandler forRuntime(WorkflowEngine engine, WorkflowRuntime runtime) {
        return new AffinityKindChangeHandler(engine, runtime::rebindAll);
    }

    public void onAffinityKindChanged(String kind) {
        workflowEngine.updateAffinityKind(kind);
        rebinder.run();
    }
}
