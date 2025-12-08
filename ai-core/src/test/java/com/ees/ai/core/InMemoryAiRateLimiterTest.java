package com.ees.ai.core;

import com.ees.ai.config.AiAgentProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class InMemoryAiRateLimiterTest {

    @Test
    void shouldEnforceRequestLimit() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRateLimit().setRequestsPerMinute(1);
        properties.getRateLimit().setTokensPerMinute(0);

        InMemoryAiRateLimiter limiter = new InMemoryAiRateLimiter(properties);
        limiter.check("user", 1);
        Assertions.assertThatThrownBy(() -> limiter.check("user", 1))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAllowWhenLimitsZero() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRateLimit().setRequestsPerMinute(0);
        properties.getRateLimit().setTokensPerMinute(0);
        InMemoryAiRateLimiter limiter = new InMemoryAiRateLimiter(properties);
        limiter.check("user", 1000);
        limiter.check("user", 1000);
    }
}
