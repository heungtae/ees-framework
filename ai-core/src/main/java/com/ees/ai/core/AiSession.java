package com.ees.ai.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 대화 세션 상태(히스토리)를 나타내는 모델.
 *
 * @param sessionId 세션 ID(널/공백 불가)
 * @param messages 누적 메시지 목록(널이면 빈 리스트)
 * @param updatedAt 마지막 업데이트 시각(널이면 현재 시각)
 */
public record AiSession(String sessionId, List<AiMessage> messages, Instant updatedAt) {

    public AiSession {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (messages == null) {
            messages = Collections.emptyList();
        } else {
            messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
