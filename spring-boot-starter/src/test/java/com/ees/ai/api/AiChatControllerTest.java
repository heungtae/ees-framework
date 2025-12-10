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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AiChatControllerTest {

    @Test
    void shouldRejectDangerousToolsWithoutApproval() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiChatController(new StubAiService(), new NullProvider<>()))
            .build();

        AiRequest request = new AiRequest("s1", "u1", List.of(new AiMessage("user", "hi")), List.of("cancelWorkflow"), false);

        try {
            MockHttpServletResponse response = mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                ).andReturn().getResponse();
            Assertions.assertThat(response.getStatus()).isEqualTo(400);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldExposeResourceEndpoints() {
        McpClient mcpClient = new McpClient() {
            @Override
            public String listNodes() {
                return "nodes";
            }

            @Override
            public String describeTopology() {
                return "topology";
            }

            @Override
            public String startWorkflow(String workflowId, java.util.Map<String, Object> params) {
                return "";
            }

            @Override
            public String pauseWorkflow(String executionId) {
                return "";
            }

            @Override
            public String resumeWorkflow(String executionId) {
                return "";
            }

            @Override
            public String cancelWorkflow(String executionId) {
                return "";
            }

            @Override
            public String getWorkflowState(String executionId) {
                return "";
            }

            @Override
            public String assignKey(String group, String partition, String kind, String key, String appId) {
                return "";
            }

            @Override
            public String lock(String name, long ttlSeconds) {
                return "";
            }

            @Override
            public String releaseLock(String name) {
                return "";
            }
        };

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiChatController(new StubAiService(), new FixedProvider<>(mcpClient)))
            .build();

        try {
            MockHttpServletResponse response = mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/ai/resources/nodes")
                ).andReturn().getResponse();
            org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(200);
            org.assertj.core.api.Assertions.assertThat(response.getContentAsString()).isEqualTo("nodes");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class StubAiService implements AiAgentService {
        @Override
        public AiResponse chat(AiRequest request) {
            return new AiResponse(request.sessionId(), "ok", false);
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
