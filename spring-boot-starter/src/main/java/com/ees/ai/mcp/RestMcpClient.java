package com.ees.ai.mcp;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * HTTP-based MCP client using WebClient. Endpoints are mapped to a simple REST shape.
 */
public class RestMcpClient implements McpClient {

    private final WebClient client;
    private final Retry retrySpec;

    public RestMcpClient(WebClient client) {
        this(client, defaultRetry());
    }

    public RestMcpClient(WebClient client, Retry retrySpec) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.retrySpec = Objects.requireNonNull(retrySpec, "retrySpec must not be null");
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
        return spec.exchangeToMono(response ->
                response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> response.statusCode().is2xxSuccessful()
                        ? Mono.just(body)
                        : Mono.error(new McpClientException(response.statusCode().value(), body))
                    )
            )
            .retryWhen(retrySpec)
            .onErrorMap(WebClientResponseException.class, ex ->
                new McpClientException(ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex))
            .onErrorMap(WebClientRequestException.class, ex ->
                new McpClientException(-1, ex.getMessage(), ex));
    }

    private static Retry defaultRetry() {
        return Retry
            .backoff(2, Duration.ofMillis(200))
            .filter(RestMcpClient::isRetryable);
    }

    private static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof McpClientException ex) {
            return ex.getStatus() == -1 || (ex.getStatus() >= 500 && ex.getStatus() < 600);
        }
        return throwable instanceof WebClientRequestException;
    }
}
