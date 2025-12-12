package com.ees.ai.mcp;

import java.util.Map;

/**
 * Placeholder MCP client that returns descriptive stubs.
 * This allows tool bridge wiring without a transport implementation.
 */
public class DefaultMcpClient implements McpClient {
    /**
     * listNodes를 수행한다.
     * @return 
     */

    @Override
    public String listNodes() {
        return "{\"status\":\"not-implemented\",\"action\":\"listNodes\"}";
    }
    /**
     * describeTopology를 수행한다.
     * @return 
     */

    @Override
    public String describeTopology() {
        return "{\"status\":\"not-implemented\",\"action\":\"describeTopology\"}";
    }
    /**
     * startWorkflow를 수행한다.
     * @param workflowId 
     * @param params 
     * @return 
     */

    @Override
    public String startWorkflow(String workflowId, Map<String, Object> params) {
        return "{\"status\":\"not-implemented\",\"action\":\"startWorkflow\",\"workflowId\":\"" + workflowId + "\"}";
    }
    /**
     * pauseWorkflow를 수행한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String pauseWorkflow(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"pauseWorkflow\",\"executionId\":\"" + executionId + "\"}";
    }
    /**
     * resumeWorkflow를 수행한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String resumeWorkflow(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"resumeWorkflow\",\"executionId\":\"" + executionId + "\"}";
    }
    /**
     * cancelWorkflow를 수행한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String cancelWorkflow(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"cancelWorkflow\",\"executionId\":\"" + executionId + "\"}";
    }
    /**
     * workflowState를 반환한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String getWorkflowState(String executionId) {
        return "{\"status\":\"not-implemented\",\"action\":\"getWorkflowState\",\"executionId\":\"" + executionId + "\"}";
    }
    /**
     * assignKey를 수행한다.
     * @param group 
     * @param partition 
     * @param kind 
     * @param key 
     * @param appId 
     * @return 
     */

    @Override
    public String assignKey(String group, String partition, String kind, String key, String appId) {
        return "{\"status\":\"not-implemented\",\"action\":\"assignKey\",\"kind\":\"" + kind + "\",\"key\":\"" + key + "\"}";
    }
    /**
     * lock를 수행한다.
     * @param name 
     * @param ttlSeconds 
     * @return 
     */

    @Override
    public String lock(String name, long ttlSeconds) {
        return "{\"status\":\"not-implemented\",\"action\":\"lock\",\"name\":\"" + name + "\"}";
    }
    /**
     * releaseLock를 수행한다.
     * @param name 
     * @return 
     */

    @Override
    public String releaseLock(String name) {
        return "{\"status\":\"not-implemented\",\"action\":\"releaseLock\",\"name\":\"" + name + "\"}";
    }
}
