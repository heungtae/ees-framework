package com.ees.ai.core;

/**
 * AI 채팅 응답 모델.
 *
 * @param sessionId 세션 ID(널/공백 불가)
 * @param content 응답 본문(널 불가)
 * @param streaming 해당 청크가 스트리밍 중간 응답인지 여부
 */
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
