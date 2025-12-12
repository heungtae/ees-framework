package com.ees.framework.workflow.engine;

import com.ees.framework.workflow.model.WorkflowDefinition;
import com.ees.framework.workflow.model.WorkflowGraphDefinition;
import com.ees.framework.workflow.util.LinearToGraphConverter;
import com.ees.framework.workflow.util.WorkflowGraphValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 여러 개의 WorkflowDefinition / WorkflowGraphDefinition 을 등록하고 실행한다.
 * - WorkflowDefinition 은 LinearToGraphConverter 를 통해 그래프로 변환 후 등록
 * - 등록 시 WorkflowGraphValidator 로 검증
 * - WorkflowEngine 을 통해 실행 가능한 Workflow 를 생성/관리
 */
@Slf4j
public class WorkflowRuntime {

    private final Map<String, Workflow> workflows = new LinkedHashMap<>();
    private final WorkflowGraphValidator validator;
    private final WorkflowEngine engine;
    private final WorkflowNodeResolver resolver;
    private final LinearToGraphConverter converter;
    private final List<WorkflowDefinition> linearDefinitions;
    private final List<WorkflowGraphDefinition> graphDefinitions;

    /**
     * 워크플로 정의를 등록하고 실행할 런타임을 생성한다.
     * 선형 정의는 그래프 형태로 변환 후 검증 및 등록되며, 모든 워크플로는 고유 이름이어야 한다.
     *
     * @param workflowDefinitions 선형 워크플로 정의 목록
     * @param workflowGraphDefinitions 그래프 워크플로 정의 목록
     * @param converter 선형 정의를 그래프로 변환할 컨버터
     * @param validator 그래프 구조를 검증할 밸리데이터
     * @param engine 실행 가능한 Workflow 생성을 담당할 엔진
     * @param resolver 노드 정의를 실제 구현체로 해석할 리졸버
     */
    public WorkflowRuntime(
        List<WorkflowDefinition> workflowDefinitions,
        List<WorkflowGraphDefinition> workflowGraphDefinitions,
        LinearToGraphConverter converter,
        WorkflowGraphValidator validator,
        WorkflowEngine engine,
        WorkflowNodeResolver resolver
    ) {
        this.converter = Objects.requireNonNull(converter, "converter must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.linearDefinitions = List.copyOf(workflowDefinitions);
        this.graphDefinitions = List.copyOf(workflowGraphDefinitions);

        this.linearDefinitions.forEach(this::registerWorkflowDefinition);
        this.graphDefinitions.forEach(this::registerGraphDefinition);
    }

    /**
     * 등록된 모든 Workflow 를 시작한다. 이미 실행 중인 워크플로는 그대로 둔다.
     */
    public void startAll() {
        workflows.values().forEach(Workflow::start);
    }

    /**
     * 등록된 모든 Workflow 를 중지한다. 실행 중이 아니어도 예외를 발생시키지 않는다.
     */
    public void stopAll() {
        workflows.values().forEach(Workflow::stop);
    }

    /**
     * 이름으로 Workflow 를 조회한다.
     *
     * @param name 워크플로 논리 이름
     * @return 매칭되는 Workflow (없으면 빈 Optional)
     */
    public Optional<Workflow> getWorkflow(String name) {
        return Optional.ofNullable(workflows.get(name));
    }

    /**
     * 등록된 Workflow 컬렉션(읽기 전용)을 반환한다.
     *
     * @return 등록된 워크플로들의 복사본
     */
    public Collection<Workflow> getWorkflows() {
        return List.copyOf(workflows.values());
    }

    /**
     * 엔진 설정 변경(예: affinity kind) 후 모든 워크플로를 중지하고 재등록/재시작한다.
     * 동기화되어 호출되므로 재바인딩 동안 외부 호출이 섞이지 않는다.
     */
    public synchronized void rebindAll() {
        stopAll();
        workflows.clear();
        linearDefinitions.forEach(this::registerWorkflowDefinition);
        graphDefinitions.forEach(this::registerGraphDefinition);
        startAll();
    }
    // registerWorkflowDefinition 동작을 수행한다.

    private void registerWorkflowDefinition(WorkflowDefinition definition) {
        WorkflowGraphDefinition graph = converter.convert(definition);
        registerGraphDefinition(graph);
    }
    // registerGraphDefinition 동작을 수행한다.

    private void registerGraphDefinition(WorkflowGraphDefinition graph) {
        validator.validate(graph);
        if (workflows.containsKey(graph.getName())) {
            throw new IllegalArgumentException("Duplicate workflow name: " + graph.getName());
        }
        Workflow workflow = engine.createWorkflow(graph, resolver);
        workflows.put(workflow.getName(), workflow);
        log.debug("Registered workflow: {}", workflow.getName());
    }
}
