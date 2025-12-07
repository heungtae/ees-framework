package com.ees.ai.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ees.ai")
public class AiAgentProperties {

    private String model = "gpt-4o-mini";

    private boolean streamingEnabled = true;

    private List<String> toolsAllowed = new ArrayList<>();

    private String historyStore = "in-memory";

    private RateLimit rateLimit = new RateLimit();

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    public List<String> getToolsAllowed() {
        return toolsAllowed;
    }

    public void setToolsAllowed(List<String> toolsAllowed) {
        this.toolsAllowed = toolsAllowed;
    }

    public String getHistoryStore() {
        return historyStore;
    }

    public void setHistoryStore(String historyStore) {
        this.historyStore = historyStore;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class RateLimit {

        private int requestsPerMinute = 60;

        private int tokensPerMinute = 0;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getTokensPerMinute() {
            return tokensPerMinute;
        }

        public void setTokensPerMinute(int tokensPerMinute) {
            this.tokensPerMinute = tokensPerMinute;
        }
    }
}
