package com.ees.metadatastore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * TTL helper utilities shared by MetadataStore implementations.
 */
public final class MetadataTtlUtils {
    // 인스턴스를 생성한다.

    private MetadataTtlUtils() {
    }

    /**
     * Calculate expiry time for the given TTL. Returns null when the TTL is zero or negative.
     */
    public static Instant expiresAt(Clock clock, Duration ttl) {
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        return clock.instant().plus(ttl);
    }

    /**
     * Determine if the provided expiry time is already passed.
     */
    public static boolean isExpired(Clock clock, Instant expiresAt) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (expiresAt == null) {
            return false;
        }
        return !expiresAt.isAfter(clock.instant());
    }
}
