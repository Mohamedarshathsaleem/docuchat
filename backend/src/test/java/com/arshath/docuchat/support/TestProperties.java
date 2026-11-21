package com.arshath.docuchat.support;

import java.nio.file.Path;
import java.time.Duration;

import com.arshath.docuchat.config.DocuChatProperties;

public final class TestProperties {

    private TestProperties() {
    }

    public static DocuChatProperties in(Path dir) {
        return new DocuChatProperties(dir,
                new DocuChatProperties.Retrieval(3, 0.1),
                new DocuChatProperties.Limits(5, Duration.ofHours(24)),
                new DocuChatProperties.RateLimit(20));
    }
}
