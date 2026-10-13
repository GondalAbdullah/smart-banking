package com.bank.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** A clock the test controls: set it to any moment, or move it forward. */
public final class MutableClock extends Clock {

    private static final ZoneId ZONE = ZoneId.of("Asia/Karachi");

    private Instant now;

    public MutableClock(LocalDateTime start) {
        set(start);
    }

    public void set(LocalDateTime time) {
        now = time.atZone(ZONE).toInstant();
    }

    public void advanceDays(long days) {
        now = now.plus(Duration.ofDays(days));
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}