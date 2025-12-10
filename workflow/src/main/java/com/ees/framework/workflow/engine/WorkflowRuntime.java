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
 * - BlockingWorkflowEngine 을 통해 실행 가능한 Workflow 를 생성/관리
 */
@Slf4j
public class WorkflowRuntime {

    private final Map<String, Workflow> workflows = new LinkedHashMap<>();
    private final WorkflowGraphValidator validator;
    private final BlockingWorkflowEngine engine;
    private final WorkflowNodeResolver resolver;
    private final LinearToGraphConverter converter;
    private final List<WorkflowDefinition> linearDefinitions;
    private final List<WorkflowGraphDefinition> graphDefinitions;

    public WorkflowRuntime(
        List<WorkflowDefinition> workflowDefinitions,
        List<WorkflowGraphDefinition> workflowGraphDefinitions,
        LinearToGraphConverter converter,
        WorkflowGraphValidator validator,
        BlockingWorkflowEngine engine,
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
     * 등록된 모든 Workflow 를 시작한다.
     */
    public void startAll() {
        workflows.values().forEach(Workflow::start);
    }

    /**
     * 등록된 모든 Workflow 를 중지한다.
     */
    public void stopAll() {
        workflows.values().forEach(Workflow::stop);
    }

    /**
     * 이름으로 Workflow 를 조회한다.
     */
    public Optional<Workflow> getWorkflow(String name) {
        return Optional.ofNullable(workflows.get(name));
    }

    /**
     * 등록된 Workflow 컬렉션(읽기 전용)을 반환한다.
     */
    public Collection<Workflow> getWorkflows() {
        return List.copyOf(workflows.values());
    }

    /**
     * Stop and rebuild all workflows using the current engine settings (e.g., updated affinity kind).
     */
    public synchronized void rebindAll() {
        stopAll();
        workflows.clear();
        linearDefinitions.forEach(this::registerWorkflowDefinition);
        graphDefinitions.forEach(this::registerGraphDefinition);
        startAll();
    }

    private void registerWorkflowDefinition(WorkflowDefinition definition) {
        WorkflowGraphDefinition graph = converter.convert(definition);
        registerGraphDefinition(graph);
    }

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
