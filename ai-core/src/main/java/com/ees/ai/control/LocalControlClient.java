package com.ees.ai.control;

import com.ees.ai.control.ControlFacade.ControlAssignKeyRequest;
import com.ees.ai.control.ControlFacade.ControlLockRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;

/**
 * Embedded 모드에서 사용되는 ControlClient 구현.
 * <p>
 * 동일 프로세스 내 {@link ControlFacade}를 직접 호출하고 결과를 JSON 문자열로 변환한다.
 */
public class LocalControlClient implements ControlClient {

    private final ControlFacade facade;
    private final ObjectMapper objectMapper;

    public LocalControlClient(ControlFacade facade, ObjectMapper objectMapper) {
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String listNodes() {
        return toJson(facade.nodes());
    }

    @Override
    public String describeTopology() {
        return toJson(facade.topology());
    }

    @Override
    public String startWorkflow(String workflowId, Map<String, Object> params) {
        return toJson(facade.startWorkflow(workflowId, params == null ? Map.of() : params));
    }

    @Override
    public String pauseWorkflow(String workflowId) {
        return toJson(facade.pauseWorkflow(workflowId));
    }

    @Override
    public String resumeWorkflow(String workflowId) {
        return toJson(facade.resumeWorkflow(workflowId));
    }

    @Override
    public String cancelWorkflow(String workflowId) {
        return toJson(facade.cancelWorkflow(workflowId));
    }

    @Override
    public String getWorkflowState(String workflowId) {
        return toJson(facade.workflowState(workflowId));
    }

    @Override
    public String assignKey(String group, String partition, String kind, String key, String appId) {
        int parsedPartition;
        try {
            parsedPartition = Integer.parseInt(partition);
        } catch (NumberFormatException ex) {
            parsedPartition = 0;
        }
        return toJson(facade.assignKey(new ControlAssignKeyRequest(group, parsedPartition, kind, key, appId)));
    }

    @Override
    public String lock(String name, long ttlSeconds) {
        return toJson(facade.lock(new ControlLockRequest(name, ttlSeconds, "ai-tool")));
    }

    @Override
    public String releaseLock(String name) {
        return toJson(Map.of("released", facade.releaseLock(name)));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize control response", ex);
        }
    }
}

