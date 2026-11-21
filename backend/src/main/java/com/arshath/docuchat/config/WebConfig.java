package com.arshath.docuchat.config;

import java.time.Clock;

import com.arshath.docuchat.ratelimit.RateLimitInterceptor;
import com.arshath.docuchat.ratelimit.RateLimiter;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(DocuChatProperties properties) {
        RateLimiter limiter = new RateLimiter(properties.rateLimit().requestsPerMinute(), Clock.systemUTC());
        this.rateLimitInterceptor = new RateLimitInterceptor(limiter);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/chat", "/api/documents");
    }
}
