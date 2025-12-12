package com.ees.ai.core;

/**
 * AI 세션 히스토리를 로드/추가하는 저장소 추상화.
 */
public interface AiSessionService {

    /**
     * 세션을 로드한다.
     *
     * @param sessionId 세션 ID
     * @return 세션
     */
    AiSession load(String sessionId);

    /**
     * 세션에 메시지를 추가한다.
     *
     * @param sessionId 세션 ID
     * @param message 추가할 메시지
     * @return 업데이트된 세션
     */
    AiSession append(String sessionId, AiMessage message);
}
