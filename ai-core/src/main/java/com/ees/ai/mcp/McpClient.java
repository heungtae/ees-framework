package com.ees.ai.mcp;

import java.util.Map;

/**
 * Minimal MCP client facade. Actual transport will be wired later.
 */
public interface McpClient {

    String listNodes();

    String describeTopology();

    String startWorkflow(String workflowId, Map<String, Object> params);

    String pauseWorkflow(String executionId);

    String resumeWorkflow(String executionId);

    String cancelWorkflow(String executionId);

    String getWorkflowState(String executionId);

    /**
     * Assign a key for the given group/partition with an explicit affinity kind.
     */
    String assignKey(String group, String partition, String kind, String key, String appId);

    String lock(String name, long ttlSeconds);

    String releaseLock(String name);
}
