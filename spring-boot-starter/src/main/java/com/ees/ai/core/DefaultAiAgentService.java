package com.ees.ai.core;

import com.ees.ai.config.AiAgentProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DefaultAiAgentService implements AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAiAgentService.class);

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final AiSessionService aiSessionService;
    private final AiToolRegistry aiToolRegistry;
    private final AiAgentProperties aiAgentProperties;
    private final AiRateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final Timer chatTimer;
    private final List<ToolCallback> toolCallbacks;

    public DefaultAiAgentService(ChatModel chatModel,
                                 StreamingChatModel streamingChatModel,
                                 AiSessionService aiSessionService,
                                 AiToolRegistry aiToolRegistry,
                                 AiAgentProperties aiAgentProperties,
                                 List<ToolCallback> toolCallbacks) {
        this(chatModel, streamingChatModel, aiSessionService, aiToolRegistry, aiAgentProperties, toolCallbacks, AiRateLimiter.NOOP, null);
    }

    public DefaultAiAgentService(ChatModel chatModel,
                                 StreamingChatModel streamingChatModel,
                                 AiSessionService aiSessionService,
                                 AiToolRegistry aiToolRegistry,
                                 AiAgentProperties aiAgentProperties,
                                 List<ToolCallback> toolCallbacks,
                                 AiRateLimiter rateLimiter,
                                 MeterRegistry meterRegistry) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.streamingChatModel = Objects.requireNonNull(streamingChatModel, "streamingChatModel must not be null");
        this.aiSessionService = Objects.requireNonNull(aiSessionService, "aiSessionService must not be null");
        this.aiToolRegistry = Objects.requireNonNull(aiToolRegistry, "aiToolRegistry must not be null");
        this.aiAgentProperties = Objects.requireNonNull(aiAgentProperties, "aiAgentProperties must not be null");
        this.rateLimiter = rateLimiter == null ? AiRateLimiter.NOOP : rateLimiter;
        this.meterRegistry = meterRegistry;
        this.chatTimer = meterRegistry != null ? meterRegistry.timer("ai.chat.duration") : null;
        this.toolCallbacks = new CopyOnWriteArrayList<>(toolCallbacks == null ? List.of() : toolCallbacks);
    }

    @Override
    public Mono<AiResponse> chat(AiRequest request) {
        if (request.streaming()) {
            if (!aiAgentProperties.isStreamingEnabled()) {
                return Mono.error(new IllegalStateException("Streaming chat is disabled by configuration."));
            }
            return chatStream(request).last();
        }
        return Mono.defer(() -> {
            int estimatedTokens = estimateTokens(request.messages());
            rateLimiter.check(request.userId(), estimatedTokens);
            long start = System.nanoTime();
            return prepareContext(request)
                .flatMap(ctx -> Mono.fromCallable(() -> chatModel.call(buildPrompt(ctx.promptMessages(), ctx.toolsAllowed())))
                    .map(this::extractContent)
                    .flatMap(content -> persistAssistantMessage(request.sessionId(), content)
                        .thenReturn(new AiResponse(request.sessionId(), content, false))))
                .doOnSuccess(resp -> {
                    recordSuccess(estimatedTokens, start);
                    audit("chat", request.userId(), resp.content(), null);
                })
                .doOnError(error -> {
                    recordError(estimatedTokens, start);
                    audit("chat", request.userId(), null, error);
                });
        });
    }

    @Override
    public Flux<AiResponse> chatStream(AiRequest request) {
        if (request.streaming()) {
            if (!aiAgentProperties.isStreamingEnabled()) {
                return Flux.error(new IllegalStateException("Streaming chat is disabled by configuration."));
            }
        }
        return Flux.defer(() -> {
            int estimatedTokens = estimateTokens(request.messages());
            rateLimiter.check(request.userId(), estimatedTokens);
            long start = System.nanoTime();
            return prepareContext(request)
                .flatMapMany(ctx -> {
                    StringBuilder buffer = new StringBuilder();
                    return streamingChatModel.stream(buildPrompt(ctx.promptMessages(), ctx.toolsAllowed()))
                        .map(this::extractContent)
                        .filter(chunk -> chunk != null && !chunk.isBlank())
                        .map(chunk -> {
                            buffer.append(chunk);
                            return new AiResponse(request.sessionId(), chunk, true);
                        })
                        .concatWith(Mono.defer(() -> persistAssistantMessage(request.sessionId(), buffer.toString())
                            .thenReturn(new AiResponse(request.sessionId(), buffer.toString(), false))));
                })
                .doOnComplete(() -> {
                    recordSuccess(estimatedTokens, start);
                    audit("chatStream", request.userId(), null, null);
                })
                .doOnError(error -> {
                    recordError(estimatedTokens, start);
                    audit("chatStream", request.userId(), null, error);
                });
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

    private Prompt buildPrompt(List<AiMessage> messages, Set<String> allowedTools) {
        List<Message> promptMessages = messages.stream()
            .map(this::toPromptMessage)
            .toList();

        // prepend a guardrail system message describing allowed tools
        if (!allowedTools.isEmpty()) {
            String toolList = String.join(", ", allowedTools);
            List<Message> augmented = new ArrayList<>();
            augmented.add(new SystemMessage("You may call tools from this allowed list only: " + toolList));
            augmented.addAll(promptMessages);
            promptMessages = augmented;
        }

        List<ToolCallback> allowedCallbacks = filterToolCallbacks(allowedTools);
        if (!allowedCallbacks.isEmpty()) {
            org.springframework.ai.model.tool.DefaultToolCallingChatOptions options =
                new org.springframework.ai.model.tool.DefaultToolCallingChatOptions();
            options.setToolCallbacks(allowedCallbacks);
            options.setToolNames(allowedTools);
            return new Prompt(promptMessages, options);
        }
        return new Prompt(promptMessages);
    }

    private List<ToolCallback> filterToolCallbacks(Set<String> allowedTools) {
        if (allowedTools.isEmpty() || toolCallbacks.isEmpty()) {
            return List.of();
        }
        return toolCallbacks.stream()
            .filter(cb -> allowedTools.contains(cb.getToolDefinition().name()))
            .toList();
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

    private int estimateTokens(List<AiMessage> messages) {
        return messages.stream()
            .mapToInt(msg -> Math.max(1, msg.content().length() / 4))
            .sum();
    }

    private void recordSuccess(int estimatedTokens, long startNano) {
        if (meterRegistry != null) {
            meterRegistry.counter("ai.chat.requests", "status", "success").increment();
            meterRegistry.counter("ai.chat.tokens", "direction", "in").increment(estimatedTokens);
            if (chatTimer != null) {
                chatTimer.record(System.nanoTime() - startNano, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        }
    }

    private void recordError(int estimatedTokens, long startNano) {
        if (meterRegistry != null) {
            meterRegistry.counter("ai.chat.requests", "status", "error").increment();
            if (chatTimer != null) {
                chatTimer.record(System.nanoTime() - startNano, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        }
    }

    private void audit(String action, String userId, Object payload, Throwable error) {
        if (error == null) {
            log.info("[AiAgentService] action={} user={} status=success payload={}", action, userId, payload);
        } else {
            log.warn("[AiAgentService] action={} user={} status=error message={}", action, userId, error.getMessage());
        }
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

    public List<ToolCallback> getToolCallbacks() {
        return toolCallbacks;
    }

    private record ChatContext(List<AiMessage> promptMessages, Set<String> toolsAllowed) {
    }
}
