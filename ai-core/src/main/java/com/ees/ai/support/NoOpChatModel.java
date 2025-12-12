package com.ees.ai.support;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * 실제 {@link ChatModel}/{@link StreamingChatModel}이 구성되지 않았을 때 사용하는 No-Op 구현.
 * <p>
 * 모든 호출은 {@link UnsupportedOperationException}을 발생시킨다.
 */
public class NoOpChatModel implements ChatModel, StreamingChatModel {

    private static final String MESSAGE = "No ChatModel/StreamingChatModel configured.";

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException 항상 발생
     */
    @Override
    public ChatResponse call(Prompt prompt) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    /**
     * {@inheritDoc}
     *
     * @return 에러 Flux
     */
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.error(new UnsupportedOperationException(MESSAGE));
    }
}
