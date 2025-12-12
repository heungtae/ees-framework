package com.ees.framework.workflow.affinity;

import com.ees.framework.workflow.engine.WorkflowEngine;
import com.ees.framework.workflow.engine.WorkflowRuntime;

import java.util.Objects;

/**
 * 클러스터가 보고한 affinity kind 변경을 워크플로 엔진에 반영하고,
 * 필요 시 워크플로를 재바인딩하는 브리지 역할을 수행한다.
 */
public class AffinityKindChangeHandler {

    private final WorkflowEngine workflowEngine;
    private final Runnable rebinder;

    /**
     * 워크플로 엔진만 주입하는 기본 생성자.
     *
     * @param workflowEngine affinity kind 를 갱신할 대상 엔진
     */
    public AffinityKindChangeHandler(WorkflowEngine workflowEngine) {
        this(workflowEngine, () -> {});
    }

    /**
     * 워크플로 엔진과 재바인딩 작업을 함께 수행할 핸들러를 구성한다.
     *
     * @param workflowEngine affinity kind 를 갱신할 대상 엔진
     * @param rebinder affinity 변경 이후 실행할 재바인딩/재시작 작업
     */
    public AffinityKindChangeHandler(WorkflowEngine workflowEngine, Runnable rebinder) {
        this.workflowEngine = Objects.requireNonNull(workflowEngine, "workflowEngine must not be null");
        this.rebinder = Objects.requireNonNull(rebinder, "rebinder must not be null");
    }

    /**
     * WorkflowRuntime 의 rebindAll 과 함께 affinity kind 를 갱신하는 헬퍼 팩토리 메서드.
     *
     * @param engine affinity kind 갱신 대상 엔진
     * @param runtime 워크플로 재바인딩을 수행할 런타임
     * @return 새 핸들러 인스턴스
     */
    public static AffinityKindChangeHandler forRuntime(WorkflowEngine engine, WorkflowRuntime runtime) {
        return new AffinityKindChangeHandler(engine, runtime::rebindAll);
    }

    /**
     * 클러스터로부터 전달받은 새로운 affinity kind 를 반영하고 재바인딩을 실행한다.
     *
     * @param kind 새 affinity kind
     */
    public void onAffinityKindChanged(String kind) {
        workflowEngine.updateAffinityKind(kind);
        rebinder.run();
    }
}
