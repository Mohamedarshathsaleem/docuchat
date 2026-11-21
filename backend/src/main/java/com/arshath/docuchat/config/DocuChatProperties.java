package com.arshath.docuchat.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "docuchat")
public record DocuChatProperties(
        @DefaultValue("./data") Path storageDir,
        @DefaultValue Retrieval retrieval,
        @DefaultValue Limits limits,
        @DefaultValue RateLimit rateLimit) {

    public record Retrieval(
            @DefaultValue("5") int topK,
            @DefaultValue("0.3") double similarityThreshold) {
    }

    public record Limits(
            @DefaultValue("50") int maxPages,
            @DefaultValue("24h") Duration documentTtl) {
    }

    public record RateLimit(@DefaultValue("20") int requestsPerMinute) {
    }
}
