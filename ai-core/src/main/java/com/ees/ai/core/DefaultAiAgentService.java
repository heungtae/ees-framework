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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Spring AI {@link ChatModel}/{@link StreamingChatModel}을 감싸는 기본 {@link AiAgentService} 구현.
 * <p>
 * 세션 히스토리 저장({@link AiSessionService}), 도구 호출 허용 목록({@link AiToolRegistry} + 설정),
 * 간단한 토큰 추정 기반 레이트 리밋({@link AiRateLimiter}), 그리고 선택적 메트릭 기록을 제공한다.
 */
public class DefaultAiAgentService implements AiAgentService {
    // logger를 반환한다.

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

    /**
     * 기본 레이트리미터/메트릭 비활성 구성으로 서비스를 생성한다.
     *
     * @param chatModel 동기 호출 모델
     * @param streamingChatModel 스트리밍 호출 모델
     * @param aiSessionService 세션 히스토리 저장소
     * @param aiToolRegistry 도구 레지스트리
     * @param aiAgentProperties 설정 프로퍼티
     * @param toolCallbacks 사용 가능한 ToolCallback 목록(옵션)
     */
    public DefaultAiAgentService(ChatModel chatModel,
                                 StreamingChatModel streamingChatModel,
                                 AiSessionService aiSessionService,
                                 AiToolRegistry aiToolRegistry,
                                 AiAgentProperties aiAgentProperties,
                                 List<ToolCallback> toolCallbacks) {
        this(chatModel, streamingChatModel, aiSessionService, aiToolRegistry, aiAgentProperties, toolCallbacks, AiRateLimiter.NOOP, null);
    }

    /**
     * 서비스 구성을 상세 지정해 생성한다.
     *
     * @param chatModel 동기 호출 모델
     * @param streamingChatModel 스트리밍 호출 모델
     * @param aiSessionService 세션 히스토리 저장소
     * @param aiToolRegistry 도구 레지스트리
     * @param aiAgentProperties 설정 프로퍼티
     * @param toolCallbacks 사용 가능한 ToolCallback 목록(옵션)
     * @param rateLimiter 레이트리미터(널이면 NOOP)
     * @param meterRegistry 메트릭 레지스트리(옵션)
     */
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

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException 스트리밍이 비활성인데 요청이 스트리밍인 경우
     * @throws RuntimeException 모델 호출 실패 등 내부 오류
     */
    @Override
    public AiResponse chat(AiRequest request) {
        if (request.streaming()) {
            if (!aiAgentProperties.isStreamingEnabled()) {
                throw new IllegalStateException("Streaming chat is disabled by configuration.");
            }
            List<AiResponse> streamed = chatStream(request);
            return streamed.isEmpty() ? new AiResponse(request.sessionId(), "", false) : streamed.get(streamed.size() - 1);
        }

        int estimatedTokens = estimateTokens(request.messages());
        rateLimiter.check(request.userId(), estimatedTokens);
        long start = System.nanoTime();
        try {
            ChatContext ctx = prepareContext(request);
            String content = extractContent(chatModel.call(buildPrompt(ctx.promptMessages(), ctx.toolsAllowed())));
            persistAssistantMessage(request.sessionId(), content);
            recordSuccess(estimatedTokens, start);
            audit("chat", request.userId(), content, null);
            return new AiResponse(request.sessionId(), content, false);
        } catch (Exception ex) {
            recordError(estimatedTokens, start);
            audit("chat", request.userId(), null, ex);
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException 스트리밍이 비활성인데 요청이 스트리밍인 경우
     * @throws RuntimeException 모델 호출 실패 등 내부 오류
     */
    @Override
    public List<AiResponse> chatStream(AiRequest request) {
        if (request.streaming() && !aiAgentProperties.isStreamingEnabled()) {
            throw new IllegalStateException("Streaming chat is disabled by configuration.");
        }

        int estimatedTokens = estimateTokens(request.messages());
        rateLimiter.check(request.userId(), estimatedTokens);
        long start = System.nanoTime();
        List<AiResponse> responses = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        try {
            ChatContext ctx = prepareContext(request);
            Prompt prompt = buildPrompt(ctx.promptMessages(), ctx.toolsAllowed());
            streamingChatModel.stream(prompt).toIterable().forEach(chunk -> {
                String content = extractContent(chunk);
                if (content != null && !content.isBlank()) {
                    buffer.append(content);
                    responses.add(new AiResponse(request.sessionId(), content, true));
                }
            });
            String full = buffer.toString();
            persistAssistantMessage(request.sessionId(), full);
            responses.add(new AiResponse(request.sessionId(), full, false));
            recordSuccess(estimatedTokens, start);
            audit("chatStream", request.userId(), null, null);
            return responses;
        } catch (Exception ex) {
            recordError(estimatedTokens, start);
            audit("chatStream", request.userId(), null, ex);
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }
    // prepareContext 동작을 수행한다.

    private ChatContext prepareContext(AiRequest request) {
        // 요청 기반으로 허용 툴 목록을 결정하고, 세션 히스토리 + 이번 요청 메시지를 결합한다.
        Set<String> allowedTools = resolveAllowedTools(request);
        AiSession session = aiSessionService.load(request.sessionId());
        persistMessages(request.sessionId(), request.messages());
        List<AiMessage> messages = combineMessages(session.messages(), request.messages());
        return new ChatContext(messages, allowedTools);
    }
    // persistAssistantMessage 동작을 수행한다.

    private void persistAssistantMessage(String sessionId, String content) {
        // 최종 응답을 assistant 메시지로 세션에 저장한다.
        if (content == null) {
            return;
        }
        aiSessionService.append(sessionId, new AiMessage("assistant", content));
    }
    // persistMessages 동작을 수행한다.

    private void persistMessages(String sessionId, List<AiMessage> messages) {
        // 사용자/시스템 메시지를 순서대로 세션에 저장한다.
        messages.forEach(message -> aiSessionService.append(sessionId, message));
    }
    // combineMessages 동작을 수행한다.

    private List<AiMessage> combineMessages(List<AiMessage> history, List<AiMessage> incoming) {
        // 히스토리(옵션) + 이번 요청 메시지를 결합한다.
        List<AiMessage> messages = new ArrayList<>();
        if (history != null) {
            messages.addAll(history);
        }
        messages.addAll(incoming);
        return messages;
    }
    // buildPrompt 동작을 수행한다.

    private Prompt buildPrompt(List<AiMessage> messages, Set<String> allowedTools) {
        // Spring AI Prompt로 변환하고, 툴 사용 가드레일 및 ToolCalling 옵션을 부여한다.
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
    // filterToolCallbacks 동작을 수행한다.

    private List<ToolCallback> filterToolCallbacks(Set<String> allowedTools) {
        // 허용 목록에 포함된 ToolCallback만 필터링한다.
        if (allowedTools.isEmpty() || toolCallbacks.isEmpty()) {
            return List.of();
        }
        return toolCallbacks.stream()
            .filter(cb -> allowedTools.contains(cb.getToolDefinition().name()))
            .toList();
    }
    // toPromptMessage 동작을 수행한다.

    private Message toPromptMessage(AiMessage message) {
        // role 문자열을 Spring AI Message 타입으로 변환한다.
        String role = message.role().toLowerCase();
        return switch (role) {
            case "system" -> new SystemMessage(message.content());
            case "assistant" -> new AssistantMessage(message.content());
            default -> new UserMessage(message.content());
        };
    }
    // extractContent 동작을 수행한다.

    private String extractContent(ChatResponse response) {
        // ChatResponse 출력 형태가 다를 수 있어 안전하게 text를 추출한다.
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
    // resolveAllowedTools 동작을 수행한다.

    private Set<String> resolveAllowedTools(AiRequest request) {
        // 레지스트리/설정/요청값을 합성해 최종 허용 도구 목록을 결정하고, 금지/미존재 도구를 방어한다.
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
    // estimateTokens 동작을 수행한다.

    private int estimateTokens(List<AiMessage> messages) {
        // 외부 토크나이저 없이 대략적인 토큰 수를 추정한다(문자 길이/4).
        return messages.stream()
            .mapToInt(msg -> Math.max(1, msg.content().length() / 4))
            .sum();
    }
    // recordSuccess 동작을 수행한다.

    private void recordSuccess(int estimatedTokens, long startNano) {
        // 성공 시 카운터/타이머 메트릭을 기록한다(옵션).
        if (meterRegistry != null) {
            meterRegistry.counter("ai.chat.requests", "status", "success").increment();
            meterRegistry.counter("ai.chat.tokens", "direction", "in").increment(estimatedTokens);
            if (chatTimer != null) {
                chatTimer.record(System.nanoTime() - startNano, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        }
    }
    // recordError 동작을 수행한다.

    private void recordError(int estimatedTokens, long startNano) {
        // 실패 시 카운터/타이머 메트릭을 기록한다(옵션).
        if (meterRegistry != null) {
            meterRegistry.counter("ai.chat.requests", "status", "error").increment();
            if (chatTimer != null) {
                chatTimer.record(System.nanoTime() - startNano, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        }
    }
    // audit 동작을 수행한다.

    private void audit(String action, String userId, Object payload, Throwable error) {
        // 최소 수준의 감사 로그를 남긴다(민감정보는 payload 설계에 따라 주의 필요).
        if (error == null) {
            log.info("[AiAgentService] action={} user={} status=success payload={}", action, userId, payload);
        } else {
            log.warn("[AiAgentService] action={} user={} status=error message={}", action, userId, error.getMessage());
        }
    }

    /**
     * 구성된 {@link AiSessionService}를 반환한다.
     */
    public AiSessionService getAiSessionService() {
        return aiSessionService;
    }

    /**
     * 구성된 {@link AiToolRegistry}를 반환한다.
     */
    public AiToolRegistry getAiToolRegistry() {
        return aiToolRegistry;
    }

    /**
     * 구성된 {@link AiAgentProperties}를 반환한다.
     */
    public AiAgentProperties getAiAgentProperties() {
        return aiAgentProperties;
    }

    /**
     * 구성된 {@link ChatModel}을 반환한다.
     */
    public ChatModel getChatModel() {
        return chatModel;
    }

    /**
     * 구성된 {@link StreamingChatModel}을 반환한다.
     */
    public StreamingChatModel getStreamingChatModel() {
        return streamingChatModel;
    }

    /**
     * 사용 가능한 ToolCallback 목록을 반환한다.
     */
    public List<ToolCallback> getToolCallbacks() {
        return toolCallbacks;
    }

    private record ChatContext(List<AiMessage> promptMessages, Set<String> toolsAllowed) {
    }
}
