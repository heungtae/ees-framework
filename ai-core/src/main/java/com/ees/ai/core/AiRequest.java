package com.ees.ai.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 채팅 요청 모델.
 *
 * @param sessionId 세션 ID(널/공백 불가)
 * @param userId 사용자 식별자(옵션)
 * @param messages 시스템/유저/어시스턴트 메시지 목록(널이면 빈 리스트)
 * @param toolsAllowed 요청에서 허용하는 도구 목록(널이면 빈 리스트)
 * @param streaming 스트리밍 응답 여부
 */
public record AiRequest(
    String sessionId,
    String userId,
    List<AiMessage> messages,
    List<String> toolsAllowed,
    boolean streaming
) {

    public AiRequest {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (messages == null) {
            messages = Collections.emptyList();
        } else {
            messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }
        if (toolsAllowed == null) {
            toolsAllowed = Collections.emptyList();
        } else {
            toolsAllowed = Collections.unmodifiableList(new ArrayList<>(toolsAllowed));
        }
    }
}
