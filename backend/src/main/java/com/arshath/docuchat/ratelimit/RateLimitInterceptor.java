package com.arshath.docuchat.ratelimit;

import com.arshath.docuchat.web.RateLimitExceededException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

/** Applies the limiter to the endpoints that cost Gemini quota (upload = embeddings, chat = generation). */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter limiter;

    public RateLimitInterceptor(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!limiter.tryAcquire(clientIp(request))) {
            throw new RateLimitExceededException(limiter.limit());
        }
        return true;
    }

    static String clientIp(HttpServletRequest request) {
        // Render / most PaaS proxies put the real client first in X-Forwarded-For
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
