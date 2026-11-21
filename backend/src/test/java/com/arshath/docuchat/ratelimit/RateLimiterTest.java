package com.arshath.docuchat.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    /** A clock the test can move forward. */
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void allowsUpToTheLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter(3, new MutableClock());

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
    }

    @Test
    void limitsEachClientSeparately() {
        RateLimiter limiter = new RateLimiter(1, new MutableClock());

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
    }

    @Test
    void windowSlidesAfterAMinute() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(1, clock);
        limiter.tryAcquire("1.1.1.1");

        clock.now = clock.now.plus(Duration.ofSeconds(61));

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
    }
}
