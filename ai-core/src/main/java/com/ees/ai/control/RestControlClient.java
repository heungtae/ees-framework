package com.ees.ai.control;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Remote 모드에서 사용되는 ControlClient 구현.
 * <p>
 * EES 애플리케이션이 노출하는 Control API(`/api/control/**`)를 HTTP로 호출한다.
 */
public class RestControlClient implements ControlClient {

    private final RestClient client;

    public RestControlClient(RestClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public String listNodes() {
        return handle(() -> client.get().uri("/api/control/nodes").retrieve().toEntity(String.class));
    }

    @Override
    public String describeTopology() {
        return handle(() -> client.get().uri("/api/control/topology").retrieve().toEntity(String.class));
    }

    @Override
    public String startWorkflow(String workflowId, Map<String, Object> params) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/api/control/workflows/{workflowId}/start").build(workflowId))
            .body(params == null ? Map.of() : params)
            .retrieve()
            .toEntity(String.class));
    }

    @Override
    public String pauseWorkflow(String workflowId) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/api/control/workflows/{workflowId}/pause").build(workflowId))
            .retrieve()
            .toEntity(String.class));
    }

    @Override
    public String resumeWorkflow(String workflowId) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/api/control/workflows/{workflowId}/resume").build(workflowId))
            .retrieve()
            .toEntity(String.class));
    }

    @Override
    public String cancelWorkflow(String workflowId) {
        return handle(() -> client.post()
            .uri(uriBuilder -> uriBuilder.path("/api/control/workflows/{workflowId}/cancel").build(workflowId))
            .retrieve()
            .toEntity(String.class));
    }

    @Override
    public String getWorkflowState(String workflowId) {
        return handle(() -> client.get()
            .uri(uriBuilder -> uriBuilder.path("/api/control/workflows/{workflowId}").build(workflowId))
            .retrieve()
            .toEntity(String.class));
    }

    @Override
    public String assignKey(String group, String partition, String kind, String key, String appId) {
        Map<String, Object> body = Map.of(
            "group", group,
            "partition", partition,
            "kind", kind,
            "key", key,
            "appId", appId
        );
        return handle(() -> client.post().uri("/api/control/assignments").body(body).retrieve().toEntity(String.class));
    }

    @Override
    public String lock(String name, long ttlSeconds) {
        Map<String, Object> body = Map.of("name", name, "ttlSeconds", ttlSeconds);
        return handle(() -> client.post().uri("/api/control/locks").body(body).retrieve().toEntity(String.class));
    }

    @Override
    public String releaseLock(String name) {
        return handle(() -> client.delete()
            .uri(uriBuilder -> uriBuilder.path("/api/control/locks/{name}").build(name))
            .retrieve()
            .toEntity(String.class));
    }

    private String handle(Supplier<ResponseEntity<String>> request) {
        try {
            ResponseEntity<String> response = request.get();
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody() == null ? "" : response.getBody();
            }
            throw new ControlClientException(response.getStatusCode().value(), response.getBody());
        } catch (RestClientResponseException ex) {
            throw new ControlClientException(ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new ControlClientException(-1, ex.getMessage(), ex);
        }
    }
}

