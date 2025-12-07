package com.ees.cluster.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public class MutableClock extends Clock {

    private Instant current;
    private final ZoneId zone;

    public MutableClock(Instant seed, ZoneId zone) {
        this.current = Objects.requireNonNull(seed, "seed must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    public void advance(Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(current, zone);
    }

    @Override
    public Instant instant() {
        return current;
    }
}
