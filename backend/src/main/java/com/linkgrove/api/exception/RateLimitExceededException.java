package com.linkgrove.api.exception;

import lombok.Getter;

/**
 * Exception thrown when rate limits are exceeded.
 */
@Getter
public class RateLimitExceededException extends RuntimeException {
    
    private final Integer retryAfterSeconds;
    
    public RateLimitExceededException(String message, Integer retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitExceededException(String message) {
        this(message, 60); // Default 60 seconds retry
    }
}

