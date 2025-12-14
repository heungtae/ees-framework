package com.ees.ai.control;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Control 연동 설정.
 * <p>
 * {@code ees.control.*} 프리픽스로 바인딩된다.
 */
@ConfigurationProperties(prefix = "ees.control")
public class ControlProperties {

    /**
     * Control 호출 모드(local|remote).
     */
    private ControlMode mode = ControlMode.LOCAL;

    /**
     * Remote 모드에서 사용할 base URL (예: http://ees-app:8081).
     */
    private String baseUrl;

    /**
     * Remote 모드에서 사용할 인증 토큰(static token).
     */
    private String authToken;

    /**
     * Remote 모드에서 사용할 HTTP 타임아웃(ms).
     */
    private long timeoutMillis = 5000;

    public ControlMode getMode() {
        return mode;
    }

    public void setMode(ControlMode mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}

