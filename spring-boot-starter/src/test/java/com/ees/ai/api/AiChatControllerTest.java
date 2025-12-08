package com.ees.ai.api;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiMessage;
import com.ees.ai.core.AiRequest;
import com.ees.ai.core.AiResponse;
import com.ees.ai.mcp.McpClient;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class AiChatControllerTest {

    @Test
    void shouldRejectDangerousToolsWithoutApproval() {
        WebTestClient client = WebTestClient.bindToController(new AiChatController(new StubAiService(), new NullProvider<>()))
            .build();

        AiRequest request = new AiRequest("s1", "u1", List.of(new AiMessage("user", "hi")), List.of("cancelWorkflow"), false);

        client.post().uri("/api/ai/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void shouldExposeResourceEndpoints() {
        McpClient mcpClient = new McpClient() {
            @Override
            public Mono<String> listNodes() {
                return Mono.just("nodes");
            }

            @Override
            public Mono<String> describeTopology() {
                return Mono.just("topology");
            }

            @Override
            public Mono<String> startWorkflow(String workflowId, java.util.Map<String, Object> params) {
                return Mono.empty();
            }

            @Override
            public Mono<String> pauseWorkflow(String executionId) {
                return Mono.empty();
            }

            @Override
            public Mono<String> resumeWorkflow(String executionId) {
                return Mono.empty();
            }

            @Override
            public Mono<String> cancelWorkflow(String executionId) {
                return Mono.empty();
            }

            @Override
            public Mono<String> getWorkflowState(String executionId) {
                return Mono.empty();
            }

            @Override
            public Mono<String> assignKey(String group, String partition, String key, String appId) {
                return Mono.empty();
            }

            @Override
            public Mono<String> lock(String name, long ttlSeconds) {
                return Mono.empty();
            }

            @Override
            public Mono<String> releaseLock(String name) {
                return Mono.empty();
            }
        };

        WebTestClient client = WebTestClient.bindToController(new AiChatController(new StubAiService(), new FixedProvider<>(mcpClient)))
            .build();

        client.get().uri("/api/ai/resources/nodes")
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(String.class).isEqualTo("nodes");
    }

    private static class StubAiService implements AiAgentService {
        @Override
        public Mono<AiResponse> chat(AiRequest request) {
            return Mono.just(new AiResponse(request.sessionId(), "ok", false));
        }
    }

    private static class NullProvider<T> implements ObjectProvider<T> {
        @Override public T getObject(Object... args) { return null; }
        @Override public T getIfAvailable() { return null; }
        @Override public T getIfUnique() { return null; }
        @Override public T getObject() { return null; }
    }

    private static class FixedProvider<T> implements ObjectProvider<T> {
        private final T value;
        FixedProvider(T value) { this.value = value; }
        @Override public T getObject(Object... args) { return value; }
        @Override public T getIfAvailable() { return value; }
        @Override public T getIfUnique() { return value; }
        @Override public T getObject() { return value; }
    }
}
