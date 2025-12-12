package com.ees.ai.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 에이전트 동작을 제어하는 설정 프로퍼티.
 * <p>
 * {@code ees.ai.*} 프리픽스로 바인딩된다.
 */
@ConfigurationProperties(prefix = "ees.ai")
public class AiAgentProperties {

    private String model = "gpt-4o-mini";

    private boolean streamingEnabled = true;

    private List<String> toolsAllowed = new ArrayList<>();

    private String historyStore = "in-memory";

    private long historyTtlSeconds = 86400;
    // RateLimit 동작을 수행한다.

    private RateLimit rateLimit = new RateLimit();

    /**
     * 사용할 모델 이름을 반환한다.
     */
    public String getModel() {
        return model;
    }

    /**
     * 사용할 모델 이름을 설정한다.
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 스트리밍 응답 허용 여부를 반환한다.
     */
    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    /**
     * 스트리밍 응답 허용 여부를 설정한다.
     */
    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    /**
     * 기본 허용 도구(툴) 목록을 반환한다.
     */
    public List<String> getToolsAllowed() {
        return toolsAllowed;
    }

    /**
     * 기본 허용 도구(툴) 목록을 설정한다.
     */
    public void setToolsAllowed(List<String> toolsAllowed) {
        this.toolsAllowed = toolsAllowed;
    }

    /**
     * 히스토리 저장소 종류를 반환한다(예: in-memory).
     */
    public String getHistoryStore() {
        return historyStore;
    }

    /**
     * 히스토리 저장소 종류를 설정한다.
     */
    public void setHistoryStore(String historyStore) {
        this.historyStore = historyStore;
    }

    /**
     * 히스토리 TTL(초)를 반환한다.
     */
    public long getHistoryTtlSeconds() {
        return historyTtlSeconds;
    }

    /**
     * 히스토리 TTL(초)를 설정한다.
     */
    public void setHistoryTtlSeconds(long historyTtlSeconds) {
        this.historyTtlSeconds = historyTtlSeconds;
    }

    /**
     * 레이트 리밋 설정을 반환한다.
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * 레이트 리밋 설정을 지정한다.
     */
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * 사용자/토큰 기반 레이트 리밋 설정.
     */
    public static class RateLimit {

        private int requestsPerMinute = 60;

        private int tokensPerMinute = 0;

        /**
         * 분당 요청 허용량을 반환한다.
         */
        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        /**
         * 분당 요청 허용량을 설정한다.
         */
        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        /**
         * 분당 토큰 허용량을 반환한다(0이면 비활성).
         */
        public int getTokensPerMinute() {
            return tokensPerMinute;
        }

        /**
         * 분당 토큰 허용량을 설정한다(0이면 비활성).
         */
        public void setTokensPerMinute(int tokensPerMinute) {
            this.tokensPerMinute = tokensPerMinute;
        }
    }
}
