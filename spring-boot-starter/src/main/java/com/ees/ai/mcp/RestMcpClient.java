package com.ees.ai.mcp;

import java.util.Map;
import java.util.Objects;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP-based MCP client using WebClient. Endpoints are mapped to a simple REST shape.
 */
public class RestMcpClient implements McpClient {

    private final WebClient client;

    public RestMcpClient(WebClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public Mono<String> listNodes() {
        return client.get().uri("/mcp/nodes").retrieve().bodyToMono(String.class);
    }

    @Override
    public Mono<String> describeTopology() {
        return client.get().uri("/mcp/topology").retrieve().bodyToMono(String.class);
    }

    @Override
    public Mono<String> startWorkflow(String workflowId, Map<String, Object> params) {
        return client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{workflowId}/start").build(workflowId))
            .body(BodyInserters.fromValue(params))
            .retrieve()
            .bodyToMono(String.class);
    }

    @Override
    public Mono<String> pauseWorkflow(String executionId) {
        return client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/pause").build(executionId))
            .retrieve()
            .bodyToMono(String.class);
    }

    @Override
    public Mono<String> resumeWorkflow(String executionId) {
        return client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/resume").build(executionId))
            .retrieve()
            .bodyToMono(String.class);
    }

    @Override
    public Mono<String> cancelWorkflow(String executionId) {
        return client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/cancel").build(executionId))
            .retrieve()
            .bodyToMono(String.class);
    }

    @Override
    public Mono<String> getWorkflowState(String executionId) {
        return client.get()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}").build(executionId))
            .retrieve()
            .bodyToMono(String.class);
    }

    @Override
    public Mono<String> assignKey(String group, String partition, String key, String appId) {
        return client.post()
            .uri("/mcp/assignments")
            .body(BodyInserters.fromValue(Map.of(
                "group", group,
                "partition", partition,
                "key", key,
                "appId", appId
            )))
            .retrieve()
            .bodyToMono(String.class);
    }

    @Override
    public Mono<String> lock(String name, long ttlSeconds) {
        return client.post()
            .uri("/mcp/locks")
            .body(BodyInserters.fromValue(Map.of(
                "name", name,
                "ttlSeconds", ttlSeconds
            )))
            .retrieve()
            .bodyToMono(String.class);
    }

    @Override
    public Mono<String> releaseLock(String name) {
        return client.delete()
            .uri(uriBuilder -> uriBuilder.path("/mcp/locks/{name}").build(name))
            .retrieve()
            .bodyToMono(String.class);
    }
}
