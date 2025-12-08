package com.ees.ai.mcp;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Placeholder MCP client that returns descriptive stubs.
 * This allows tool bridge wiring without a transport implementation.
 */
public class DefaultMcpClient implements McpClient {

    @Override
    public Mono<String> listNodes() {
        return Mono.just("listNodes is not implemented yet.");
    }

    @Override
    public Mono<String> describeTopology() {
        return Mono.just("describeTopology is not implemented yet.");
    }

    @Override
    public Mono<String> startWorkflow(String workflowId, Map<String, Object> params) {
        return Mono.just("startWorkflow(" + workflowId + ") is not implemented yet.");
    }

    @Override
    public Mono<String> pauseWorkflow(String executionId) {
        return Mono.just("pauseWorkflow(" + executionId + ") is not implemented yet.");
    }

    @Override
    public Mono<String> resumeWorkflow(String executionId) {
        return Mono.just("resumeWorkflow(" + executionId + ") is not implemented yet.");
    }

    @Override
    public Mono<String> cancelWorkflow(String executionId) {
        return Mono.just("cancelWorkflow(" + executionId + ") is not implemented yet.");
    }

    @Override
    public Mono<String> getWorkflowState(String executionId) {
        return Mono.just("getWorkflowState(" + executionId + ") is not implemented yet.");
    }

    @Override
    public Mono<String> assignKey(String group, String partition, String key, String appId) {
        return Mono.just("assignKey(" + key + ") is not implemented yet.");
    }

    @Override
    public Mono<String> lock(String name, long ttlSeconds) {
        return Mono.just("lock(" + name + ") is not implemented yet.");
    }

    @Override
    public Mono<String> releaseLock(String name) {
        return Mono.just("releaseLock(" + name + ") is not implemented yet.");
    }
}
