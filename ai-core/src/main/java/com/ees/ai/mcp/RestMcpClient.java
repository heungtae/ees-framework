package com.ees.ai.mcp;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP-based MCP client using RestClient. Endpoints are mapped to a simple REST shape.
 */
public class RestMcpClient implements McpClient {

    private final RestClient client;
    /**
     * 인스턴스를 생성한다.
     * @param client 
     */

    public RestMcpClient(RestClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }
    /**
     * listNodes를 수행한다.
     * @return 
     */

    @Override
    public String listNodes() {
        return handle(() -> client.get().uri("/mcp/nodes").retrieve().toEntity(String.class));
    }
    /**
     * describeTopology를 수행한다.
     * @return 
     */

    @Override
    public String describeTopology() {
        return handle(() -> client.get().uri("/mcp/topology").retrieve().toEntity(String.class));
    }
    /**
     * startWorkflow를 수행한다.
     * @param workflowId 
     * @param params 
     * @return 
     */

    @Override
    public String startWorkflow(String workflowId, Map<String, Object> params) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{workflowId}/start").build(workflowId))
            .body(params)
            .retrieve()
            .toEntity(String.class));
    }
    /**
     * pauseWorkflow를 수행한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String pauseWorkflow(String executionId) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/pause").build(executionId))
            .retrieve()
            .toEntity(String.class));
    }
    /**
     * resumeWorkflow를 수행한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String resumeWorkflow(String executionId) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/resume").build(executionId))
            .retrieve()
            .toEntity(String.class));
    }
    /**
     * cancelWorkflow를 수행한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String cancelWorkflow(String executionId) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}/cancel").build(executionId))
            .retrieve()
            .toEntity(String.class));
    }
    /**
     * workflowState를 반환한다.
     * @param executionId 
     * @return 
     */

    @Override
    public String getWorkflowState(String executionId) {
        return handle(() -> client.get()
            .uri(uriBuilder -> uriBuilder.path("/mcp/workflows/{executionId}").build(executionId))
            .retrieve()
            .toEntity(String.class));
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
        return handle(() -> client.post()
            .uri("/mcp/assignments")
            .body(Map.of(
                "group", group,
                "partition", partition,
                "kind", kind,
                "key", key,
                "appId", appId
            ))
            .retrieve()
            .toEntity(String.class));
    }
    /**
     * lock를 수행한다.
     * @param name 
     * @param ttlSeconds 
     * @return 
     */

    @Override
    public String lock(String name, long ttlSeconds) {
        return handle(() -> client.post()
            .uri("/mcp/locks")
            .body(Map.of(
                "name", name,
                "ttlSeconds", ttlSeconds
            ))
            .retrieve()
            .toEntity(String.class));
    }
    /**
     * releaseLock를 수행한다.
     * @param name 
     * @return 
     */

    @Override
    public String releaseLock(String name) {
        return handle(() -> client.delete()
            .uri(uriBuilder -> uriBuilder.path("/mcp/locks/{name}").build(name))
            .retrieve()
            .toEntity(String.class));
    }
    // handle 동작을 수행한다.

    private String handle(Supplier<ResponseEntity<String>> request) {
        try {
            ResponseEntity<String> response = request.get();
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody() == null ? "" : response.getBody();
            }
            throw new McpClientException(response.getStatusCode().value(), response.getBody());
        } catch (RestClientResponseException ex) {
            throw new McpClientException(ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new McpClientException(-1, ex.getMessage(), ex);
        }
    }
}
