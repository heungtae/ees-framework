package com.ees.ai.support;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public class NoOpChatModel implements ChatModel, StreamingChatModel {

    private static final String MESSAGE = "No ChatModel/StreamingChatModel configured.";

    @Override
    public ChatResponse call(Prompt prompt) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.error(new UnsupportedOperationException(MESSAGE));
    }
}
