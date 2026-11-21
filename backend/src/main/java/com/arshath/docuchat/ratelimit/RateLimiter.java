package com.arshath.docuchat.ratelimit;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window limiter keyed by client IP. Protects the free Gemini quota of a public demo.
 * In-memory is fine for one instance; several instances would need Redis.
 */
public class RateLimiter {

    private static final long WINDOW_MILLIS = 60_000;

    private final int limit;
    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(int limitPerMinute, Clock clock) {
        this.limit = limitPerMinute;
        this.clock = clock;
    }

    public boolean tryAcquire(String key) {
        long now = clock.millis();
        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    public int limit() {
        return limit;
    }
}
