package com.arshath.docuchat.web;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(int limit) {
        super("Rate limit reached (" + limit + " requests per minute). Please wait a moment.");
    }
}
