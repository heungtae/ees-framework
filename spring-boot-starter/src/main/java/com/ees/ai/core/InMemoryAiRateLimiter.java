package com.ees.ai.core;

import com.ees.ai.config.AiAgentProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Naive per-user per-minute rate limiter (requests/tokens).
 */
public class InMemoryAiRateLimiter implements AiRateLimiter {

    private final int requestsPerMinute;
    private final int tokensPerMinute;
    private final Clock clock;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryAiRateLimiter(AiAgentProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public InMemoryAiRateLimiter(AiAgentProperties properties, Clock clock) {
        this.requestsPerMinute = properties.getRateLimit().getRequestsPerMinute();
        this.tokensPerMinute = properties.getRateLimit().getTokensPerMinute();
        this.clock = clock;
    }

    @Override
    public void check(String userId, int estimatedTokens) {
        if (requestsPerMinute <= 0 && tokensPerMinute <= 0) {
            return;
        }
        String key = userId == null || userId.isBlank() ? "anonymous" : userId;
        Instant now = clock.instant();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart.plusSeconds(60))) {
                return new Window(now, new AtomicInteger(), new AtomicInteger());
            }
            return existing;
        });
        int reqCount = window.requests.incrementAndGet();
        if (requestsPerMinute > 0 && reqCount > requestsPerMinute) {
            throw new IllegalStateException("AI rate limit exceeded: " + requestsPerMinute + " requests/min for " + key);
        }
        if (tokensPerMinute > 0) {
            int tokenCount = window.tokens.addAndGet(Math.max(1, estimatedTokens));
            if (tokenCount > tokensPerMinute) {
                throw new IllegalStateException("AI token limit exceeded: " + tokensPerMinute + " tokens/min for " + key);
            }
        }
    }

    private record Window(Instant windowStart, AtomicInteger requests, AtomicInteger tokens) {
    }
}
