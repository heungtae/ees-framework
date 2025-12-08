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
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"listNodes\"}");
    }

    @Override
    public Mono<String> describeTopology() {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"describeTopology\"}");
    }

    @Override
    public Mono<String> startWorkflow(String workflowId, Map<String, Object> params) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"startWorkflow\",\"workflowId\":\"" + workflowId + "\"}");
    }

    @Override
    public Mono<String> pauseWorkflow(String executionId) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"pauseWorkflow\",\"executionId\":\"" + executionId + "\"}");
    }

    @Override
    public Mono<String> resumeWorkflow(String executionId) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"resumeWorkflow\",\"executionId\":\"" + executionId + "\"}");
    }

    @Override
    public Mono<String> cancelWorkflow(String executionId) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"cancelWorkflow\",\"executionId\":\"" + executionId + "\"}");
    }

    @Override
    public Mono<String> getWorkflowState(String executionId) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"getWorkflowState\",\"executionId\":\"" + executionId + "\"}");
    }

    @Override
    public Mono<String> assignKey(String group, String partition, String key, String appId) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"assignKey\",\"key\":\"" + key + "\"}");
    }

    @Override
    public Mono<String> lock(String name, long ttlSeconds) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"lock\",\"name\":\"" + name + "\"}");
    }

    @Override
    public Mono<String> releaseLock(String name) {
        return Mono.just("{\"status\":\"not-implemented\",\"action\":\"releaseLock\",\"name\":\"" + name + "\"}");
    }
}
