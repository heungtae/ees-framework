package com.ees.ai.mcp;

import java.util.Map;
import java.util.Objects;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
        return handle(client.get().uri("/mcp/nodes"));
    }

    @Override
    public Mono<String> describeTopology() {
        return handle(client.get().uri("/mcp/topology"));
    }

    @Override
    public Mono<String> startWorkflow(String workflowId, Map<String, Object> params) {
        return handle(client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{workflowId}/start").build(workflowId))
            .body(BodyInserters.fromValue(params)));
    }

    @Override
    public Mono<String> pauseWorkflow(String executionId) {
        return handle(client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/pause").build(executionId))
        );
    }

    @Override
    public Mono<String> resumeWorkflow(String executionId) {
        return handle(client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/resume").build(executionId))
        );
    }

    @Override
    public Mono<String> cancelWorkflow(String executionId) {
        return handle(client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/cancel").build(executionId))
        );
    }

    @Override
    public Mono<String> getWorkflowState(String executionId) {
        return handle(client.get()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}").build(executionId))
        );
    }

    @Override
    public Mono<String> assignKey(String group, String partition, String key, String appId) {
        return handle(client.post()
            .uri("/mcp/assignments")
            .body(BodyInserters.fromValue(Map.of(
                "group", group,
                "partition", partition,
                "key", key,
                "appId", appId
            ))));
    }

    @Override
    public Mono<String> lock(String name, long ttlSeconds) {
        return handle(client.post()
            .uri("/mcp/locks")
            .body(BodyInserters.fromValue(Map.of(
                "name", name,
                "ttlSeconds", ttlSeconds
            ))));
    }

    @Override
    public Mono<String> releaseLock(String name) {
        return handle(client.delete()
            .uri(uriBuilder -> uriBuilder.path("/mcp/locks/{name}").build(name)));
    }

    private Mono<String> handle(WebClient.RequestHeadersSpec<?> spec) {
        return spec
            .retrieve()
            .onStatus(
                status -> status.isError(),
                response -> response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> new IllegalStateException("MCP HTTP " + response.statusCode().value() + ": " + body))
            )
            .bodyToMono(String.class)
            .onErrorMap(WebClientResponseException.class, ex ->
                new IllegalStateException("MCP HTTP " + ex.getRawStatusCode() + ": " + ex.getResponseBodyAsString(), ex));
    }
}
