package com.ees.ai.core;

/**
 * Simple interface to enforce per-user AI rate limits.
 */
public interface AiRateLimiter {

    void check(String userId, int estimatedTokens);

    AiRateLimiter NOOP = (userId, tokens) -> {
    };
}
