package com.ees.ai.core;

/**
 * AI 대화 메시지 단위(role + content).
 *
 * @param role 메시지 역할(예: {@code system}, {@code user}, {@code assistant})
 * @param content 메시지 본문
 */
public record AiMessage(String role, String content) {

    public AiMessage {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }
}
