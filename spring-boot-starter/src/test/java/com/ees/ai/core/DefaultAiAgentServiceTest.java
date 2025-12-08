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

        DefaultAiAgentService service = new DefaultAiAgentService(chatModel, streamingModel, sessionService, toolRegistry, props);

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

        DefaultAiAgentService service = new DefaultAiAgentService(chatModel, streamingModel, sessionService, toolRegistry, props);

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

        DefaultAiAgentService service = new DefaultAiAgentService(chatModel, streamingModel, sessionService, toolRegistry, props);

        AiRequest request = new AiRequest("sess-3", "user-3", List.of(new AiMessage("user", "hi")), List.of("deleteAll"), false);

        Assertions.assertThatThrownBy(() -> service.chat(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown tools");
    }

    private static class StubChatModel implements ChatModel {

        private final List<Generation> generations;

        StubChatModel(List<Generation> generations) {
            this.generations = generations;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
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
