package com.ees.ai.core;

/**
 * AI 모델/에이전트와의 대화를 수행하는 서비스 추상화.
 * <p>
 * 기본 구현은 단일 응답({@link #chat(AiRequest)})을 제공하며,
 * 구현체가 스트리밍을 지원하는 경우 {@link #chatStream(AiRequest)}를 오버라이드할 수 있다.
 */
public interface AiAgentService {

    /**
     * 단일 응답 형태로 채팅을 수행한다.
     *
     * @param request 요청
     * @return 응답
     */
    AiResponse chat(AiRequest request);

    /**
     * 스트리밍 응답을 수행한다.
     * <p>
     * 기본 구현은 {@link #chat(AiRequest)} 결과를 단일 항목 리스트로 감싸 반환한다.
     *
     * @param request 요청
     * @return 스트리밍 청크 목록(마지막 항목은 최종 결과가 될 수 있음)
     */
    default java.util.List<AiResponse> chatStream(AiRequest request) {
        return java.util.List.of(chat(request));
    }
}
