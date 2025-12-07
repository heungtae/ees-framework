package com.ees.ai.core;

public record AiResponse(String sessionId, String content, boolean streaming) {

    public AiResponse {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }
}
