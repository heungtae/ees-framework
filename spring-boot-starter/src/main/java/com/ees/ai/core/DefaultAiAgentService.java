package com.ees.ai.core;

import com.ees.ai.config.AiAgentProperties;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultAiAgentService implements AiAgentService {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final AiSessionService aiSessionService;
    private final AiToolRegistry aiToolRegistry;
    private final AiAgentProperties aiAgentProperties;

    public DefaultAiAgentService(ChatModel chatModel,
                                 StreamingChatModel streamingChatModel,
                                 AiSessionService aiSessionService,
                                 AiToolRegistry aiToolRegistry,
                                 AiAgentProperties aiAgentProperties) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.aiSessionService = aiSessionService;
        this.aiToolRegistry = aiToolRegistry;
        this.aiAgentProperties = aiAgentProperties;
    }

    @Override
    public Mono<AiResponse> chat(AiRequest request) {
        if (request.streaming()) {
            if (!aiAgentProperties.isStreamingEnabled()) {
                return Mono.error(new IllegalStateException("Streaming chat is disabled by configuration."));
            }
            return chatStream(request).last();
        }
        return prepareContext(request)
            .flatMap(ctx -> Mono.fromCallable(() -> chatModel.call(buildPrompt(ctx.promptMessages())))
                .map(this::extractContent)
                .flatMap(content -> persistAssistantMessage(request.sessionId(), content)
                    .thenReturn(new AiResponse(request.sessionId(), content, false))));
    }

    @Override
    public Flux<AiResponse> chatStream(AiRequest request) {
        if (request.streaming()) {
            if (!aiAgentProperties.isStreamingEnabled()) {
                return Flux.error(new IllegalStateException("Streaming chat is disabled by configuration."));
            }
        }
        return prepareContext(request)
            .flatMapMany(ctx -> {
                StringBuilder buffer = new StringBuilder();
                return streamingChatModel.stream(buildPrompt(ctx.promptMessages()))
                    .map(this::extractContent)
                    .filter(chunk -> chunk != null && !chunk.isBlank())
                    .map(chunk -> {
                        buffer.append(chunk);
                        return new AiResponse(request.sessionId(), chunk, true);
                    })
                    .concatWith(Mono.defer(() -> persistAssistantMessage(request.sessionId(), buffer.toString())
                        .thenReturn(new AiResponse(request.sessionId(), buffer.toString(), false))));
            });
    }

    private Mono<ChatContext> prepareContext(AiRequest request) {
        Set<String> allowedTools = resolveAllowedTools(request);
        return aiSessionService.load(request.sessionId())
            .flatMap(session -> persistMessages(request.sessionId(), request.messages())
                .thenReturn(combineMessages(session.messages(), request.messages()))
                .map(messages -> new ChatContext(messages, allowedTools)));
    }

    private Mono<Void> persistAssistantMessage(String sessionId, String content) {
        if (content == null) {
            return Mono.empty();
        }
        return aiSessionService.append(sessionId, new AiMessage("assistant", content)).then();
    }

    private Mono<Void> persistMessages(String sessionId, List<AiMessage> messages) {
        return Flux.fromIterable(messages)
            .flatMap(message -> aiSessionService.append(sessionId, message))
            .then();
    }

    private List<AiMessage> combineMessages(List<AiMessage> history, List<AiMessage> incoming) {
        List<AiMessage> messages = new ArrayList<>();
        if (history != null) {
            messages.addAll(history);
        }
        messages.addAll(incoming);
        return messages;
    }

    private Prompt buildPrompt(List<AiMessage> messages) {
        List<Message> promptMessages = messages.stream()
            .map(this::toPromptMessage)
            .toList();
        return new Prompt(promptMessages);
    }

    private Message toPromptMessage(AiMessage message) {
        String role = message.role().toLowerCase();
        return switch (role) {
            case "system" -> new SystemMessage(message.content());
            case "assistant" -> new AssistantMessage(message.content());
            default -> new UserMessage(message.content());
        };
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return "";
        }
        Object output = response.getResult().getOutput();
        if (output instanceof org.springframework.ai.chat.messages.AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }
        if (output instanceof org.springframework.ai.chat.messages.AbstractMessage message) {
            return message.getText();
        }
        return output != null ? output.toString() : "";
    }

    private Set<String> resolveAllowedTools(AiRequest request) {
        Set<String> registryTools = aiToolRegistry.list().stream()
            .map(AiTool::name)
            .collect(Collectors.toSet());

        Set<String> configuredWhitelist = new HashSet<>(aiAgentProperties.getToolsAllowed());
        Set<String> requested = request.toolsAllowed().isEmpty()
            ? new HashSet<>(configuredWhitelist.isEmpty() ? registryTools : configuredWhitelist)
            : new HashSet<>(request.toolsAllowed());

        Set<String> unknown = requested.stream()
            .filter(name -> !registryTools.isEmpty() && !registryTools.contains(name))
            .collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown tools requested: " + String.join(", ", unknown));
        }

        if (!configuredWhitelist.isEmpty()) {
            Set<String> disallowed = requested.stream()
                .filter(name -> !configuredWhitelist.contains(name))
                .collect(Collectors.toSet());
            if (!disallowed.isEmpty()) {
                throw new IllegalArgumentException("Tools not allowed by configuration: " + String.join(", ", disallowed));
            }
        }
        return requested;
    }

    public AiSessionService getAiSessionService() {
        return aiSessionService;
    }

    public AiToolRegistry getAiToolRegistry() {
        return aiToolRegistry;
    }

    public AiAgentProperties getAiAgentProperties() {
        return aiAgentProperties;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public StreamingChatModel getStreamingChatModel() {
        return streamingChatModel;
    }

    private record ChatContext(List<AiMessage> promptMessages, Set<String> toolsAllowed) {
    }
}
