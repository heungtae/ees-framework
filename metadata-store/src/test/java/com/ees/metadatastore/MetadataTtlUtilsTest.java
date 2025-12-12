package com.ees.metadatastore;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataTtlUtilsTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void expiresAtReturnsNullForNonPositiveTtl() {
        assertThat(MetadataTtlUtils.expiresAt(fixedClock, Duration.ZERO)).isNull();
        assertThat(MetadataTtlUtils.expiresAt(fixedClock, Duration.ofSeconds(-1))).isNull();
    }

    @Test
    void expiresAtAddsDuration() {
        Instant expires = MetadataTtlUtils.expiresAt(fixedClock, Duration.ofSeconds(5));
        assertThat(expires).isEqualTo(Instant.parse("2024-01-01T00:00:05Z"));
    }

    @Test
    void isExpiredDetectsPastAndFuture() {
        Instant future = Instant.parse("2024-01-01T00:00:10Z");
        Instant past = Instant.parse("2023-12-31T23:59:59Z");

        assertThat(MetadataTtlUtils.isExpired(fixedClock, future)).isFalse();
        assertThat(MetadataTtlUtils.isExpired(fixedClock, past)).isTrue();
        assertThat(MetadataTtlUtils.isExpired(fixedClock, null)).isFalse();
    }
}
