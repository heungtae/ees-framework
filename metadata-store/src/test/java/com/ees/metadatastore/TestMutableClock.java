package com.ees.metadatastore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

class TestMutableClock extends Clock {

    private Instant now;
    private final ZoneId zoneId;

    private TestMutableClock(Instant seed, ZoneId zoneId) {
        this.now = seed;
        this.zoneId = zoneId;
    }

    static TestMutableClock fixedNow() {
        return new TestMutableClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TestMutableClock(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
