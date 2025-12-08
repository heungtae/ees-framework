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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
