package com.ees.ai.mcp;

import java.util.Map;

/**
 * Placeholder MCP client that returns descriptive stubs.
 * This allows tool bridge wiring without a transport implementation.
 */
public class DefaultMcpClient implements McpClient {

    @Override
    public String listNodes() {
        return "{\"status\":\"not-implemented\",\"action\":\"listNodes\"}";
    }

    @Override
    public String describeTopology() {
        return "{\"status\":\"not-implemented\",\"action\":\"describeTopology\"}";
    }

    @Override
    public String startWorkflow(String workflowId, Map<String, Object> params) {
        return "{\"status\":\"not-implemented\",\"action\":\"startWorkflow\",\"workflowId\":\"" + workflowId + "\"}";
    }

    @Override
    public String pauseWorkflow(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"pauseWorkflow\",\"executionId\":\"" + executionId + "\"}";
    }

    @Override
    public String resumeWorkflow(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"resumeWorkflow\",\"executionId\":\"" + executionId + "\"}";
    }

    @Override
    public String cancelWorkflow(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"cancelWorkflow\",\"executionId\":\"" + executionId + "\"}";
    }

    @Override
    public String getWorkflowState(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"getWorkflowState\",\"executionId\":\"" + executionId + "\"}";
    }

    @Override
    public String assignKey(String group, String partition, String kind, String key, String appId) {
        return "{\"status\":\"not-implemented\",\"action\":\"assignKey\",\"kind\":\"" + kind + "\",\"key\":\"" + key + "\"}";
    }

    @Override
    public String lock(String name, long ttlSeconds) {
        return "{\"status\":\"not-implemented\",\"action\":\"lock\",\"name\":\"" + name + "\"}";
    }

    @Override
    public String releaseLock(String name) {
        return "{\"status\":\"not-implemented\",\"action\":\"releaseLock\",\"name\":\"" + name + "\"}";
    }
}
