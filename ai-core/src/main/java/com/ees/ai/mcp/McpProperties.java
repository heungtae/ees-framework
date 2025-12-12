package com.ees.ai.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ees.mcp")
public class McpProperties {

    /**
     * Base URL for MCP API endpoints (e.g., http://localhost:8080/api/mcp).
     */
    private String baseUrl;

    /**
     * Timeout milliseconds for MCP HTTP calls.
     */
    private long timeoutMillis = 5000;

    /**
     * Optional Bearer token for MCP HTTP calls.
     */
    private String authToken;
    /**
     * baseUrl를 반환한다.
     * @return 
     */

    public String getBaseUrl() {
        return baseUrl;
    }
    /**
     * baseUrl를 설정한다.
     * @param baseUrl 
     */

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    /**
     * timeoutMillis를 반환한다.
     * @return 
     */

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
    /**
     * timeoutMillis를 설정한다.
     * @param timeoutMillis 
     */

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
    /**
     * authToken를 반환한다.
     * @return 
     */

    public String getAuthToken() {
        return authToken;
    }
    /**
     * authToken를 설정한다.
     * @param authToken 
     */

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
