package com.ees.ai.mcp;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Minimal MCP client facade. Actual transport will be wired later.
 */
public interface McpClient {

    Mono<String> listNodes();

    Mono<String> describeTopology();

    Mono<String> startWorkflow(String workflowId, Map<String, Object> params);

    Mono<String> pauseWorkflow(String executionId);

    Mono<String> resumeWorkflow(String executionId);

    Mono<String> cancelWorkflow(String executionId);

    Mono<String> getWorkflowState(String executionId);

    Mono<String> assignKey(String group, String partition, String key, String appId);

    Mono<String> lock(String name, long ttlSeconds);

    Mono<String> releaseLock(String name);
}
