package com.ees.ai.core;

import com.ees.ai.config.AiAgentProperties;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import reactor.core.publisher.Mono;

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
            return Mono.error(new UnsupportedOperationException("Streaming chat is not implemented yet."));
        }
        return Mono.error(new UnsupportedOperationException("Chat is not implemented yet."));
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
}
