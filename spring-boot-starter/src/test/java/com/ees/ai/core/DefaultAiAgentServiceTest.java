package com.ees.ai.core;

import com.ees.ai.config.AiAgentProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class DefaultAiAgentServiceTest {

    @Test
    void chatShouldPersistMessagesAndReturnResponse() {
        InMemoryAiSessionService sessionService = new InMemoryAiSessionService();
        DefaultAiToolRegistry toolRegistry = new DefaultAiToolRegistry();
        AiAgentProperties props = new AiAgentProperties();
        StubChatModel chatModel = new StubChatModel(List.of(new Generation(new AssistantMessage("hello"))));
        StubStreamingChatModel streamingModel = new StubStreamingChatModel(List.of());

        DefaultAiAgentService service = new DefaultAiAgentService(chatModel, streamingModel, sessionService, toolRegistry, props, List.of());

        AiRequest request = new AiRequest("sess-1", "user-1", List.of(new AiMessage("user", "hi")), List.of(), false);

        StepVerifier.create(service.chat(request))
            .assertNext(resp -> {
                Assertions.assertThat(resp.content()).isEqualTo("hello");
                Assertions.assertThat(resp.streaming()).isFalse();
            })
            .verifyComplete();

        StepVerifier.create(sessionService.load("sess-1"))
            .assertNext(session -> Assertions.assertThat(session.messages())
                .extracting(AiMessage::content)
                .containsExactly("hi", "hello"))
            .verifyComplete();
    }

    @Test
    void chatStreamShouldEmitChunksAndPersistFinalMessage() {
        InMemoryAiSessionService sessionService = new InMemoryAiSessionService();
        DefaultAiToolRegistry toolRegistry = new DefaultAiToolRegistry();
        AiAgentProperties props = new AiAgentProperties();
        StubChatModel chatModel = new StubChatModel(List.of(new Generation(new AssistantMessage("unused"))));
        StubStreamingChatModel streamingModel = new StubStreamingChatModel(List.of(
            new ChatResponse(List.of(new Generation(new AssistantMessage("he")))),
            new ChatResponse(List.of(new Generation(new AssistantMessage("llo"))))
        ));

        DefaultAiAgentService service = new DefaultAiAgentService(chatModel, streamingModel, sessionService, toolRegistry, props, List.of());

        AiRequest request = new AiRequest("sess-2", "user-2", List.of(new AiMessage("user", "hi")), List.of(), true);

        StepVerifier.create(service.chatStream(request))
            .expectNextMatches(resp -> resp.streaming() && resp.content().equals("he"))
            .expectNextMatches(resp -> resp.streaming() && resp.content().equals("llo"))
            .expectNextMatches(resp -> !resp.streaming() && resp.content().equals("hello"))
            .verifyComplete();

        StepVerifier.create(sessionService.load("sess-2"))
            .assertNext(session -> Assertions.assertThat(session.messages())
                .extracting(AiMessage::content)
                .containsExactly("hi", "hello"))
            .verifyComplete();
    }

    @Test
    void chatShouldRejectUnknownTools() {
        InMemoryAiSessionService sessionService = new InMemoryAiSessionService();
        DefaultAiToolRegistry toolRegistry = new DefaultAiToolRegistry();
        toolRegistry.register(new AiTool("listNodes", "List nodes"));
        AiAgentProperties props = new AiAgentProperties();
        StubChatModel chatModel = new StubChatModel(List.of(new Generation(new AssistantMessage("ok"))));
        StubStreamingChatModel streamingModel = new StubStreamingChatModel(List.of());

        DefaultAiAgentService service = new DefaultAiAgentService(chatModel, streamingModel, sessionService, toolRegistry, props, List.of());

        AiRequest request = new AiRequest("sess-3", "user-3", List.of(new AiMessage("user", "hi")), List.of("deleteAll"), false);

        Assertions.assertThatThrownBy(() -> service.chat(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown tools");
    }

    @Test
    void shouldInjectAllowedToolsGuardAndCallbacks() {
        InMemoryAiSessionService sessionService = new InMemoryAiSessionService();
        DefaultAiToolRegistry toolRegistry = new DefaultAiToolRegistry();
        toolRegistry.register(new AiTool("listNodes", "List nodes"));
        toolRegistry.register(new AiTool("describeTopology", "Describe topology"));
        AiAgentProperties props = new AiAgentProperties();

        StubChatModel chatModel = new StubChatModel(List.of(new Generation(new AssistantMessage("ok"))));
        StubStreamingChatModel streamingModel = new StubStreamingChatModel(List.of());

        ToolCallback tool1 = toolCallback("listNodes");
        ToolCallback tool2 = toolCallback("describeTopology");

        DefaultAiAgentService service = new DefaultAiAgentService(
            chatModel, streamingModel, sessionService, toolRegistry, props, List.of(tool1, tool2));

        AiRequest request = new AiRequest("sess-4", "user-4", List.of(new AiMessage("user", "hi")), List.of("listNodes"), false);

        service.chat(request).block();

        Prompt prompt = chatModel.lastPrompt;
        Assertions.assertThat(prompt.getInstructions().get(0))
            .isInstanceOfSatisfying(org.springframework.ai.chat.messages.SystemMessage.class,
                m -> Assertions.assertThat(m.getText()).contains("listNodes"));

        org.springframework.ai.model.tool.DefaultToolCallingChatOptions options =
            (org.springframework.ai.model.tool.DefaultToolCallingChatOptions) prompt.getOptions();

        Assertions.assertThat(options.getToolCallbacks())
            .hasSize(1)
            .first()
            .extracting(cb -> cb.getToolDefinition().name())
            .isEqualTo("listNodes");
        Assertions.assertThat(options.getToolNames()).containsExactly("listNodes");
    }

    private ToolCallback toolCallback(String name) {
        return new ToolCallback() {
            private final org.springframework.ai.tool.definition.ToolDefinition definition =
                DefaultToolDefinition.builder()
                    .name(name)
                    .description(name)
                    .inputSchema("{}")
                    .build();

            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String request) {
                return "";
            }
        };
    }

    private static class StubChatModel implements ChatModel {

        private final List<Generation> generations;
        Prompt lastPrompt;

        StubChatModel(List<Generation> generations) {
            this.generations = generations;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            this.lastPrompt = prompt;
            return new ChatResponse(generations);
        }
    }

    private static class StubStreamingChatModel implements StreamingChatModel {

        private final List<ChatResponse> responses;

        StubStreamingChatModel(List<ChatResponse> responses) {
            this.responses = new CopyOnWriteArrayList<>(responses);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.fromIterable(responses);
        }
    }
}
