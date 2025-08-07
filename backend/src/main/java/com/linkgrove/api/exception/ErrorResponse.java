package com.linkgrove.api.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response structure for API errors.
 * Provides consistent error information across all endpoints.
 */
@Data
@Builder
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred
     */
    private Instant timestamp;
    
    /**
     * HTTP status code
     */
    private Integer status;
    
    /**
     * Error type/category
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Request path where the error occurred
     */
    private String path;
    
    /**
     * Field-level validation errors (optional)
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Retry-after seconds for rate limiting (optional)
     */
    private Integer retryAfter;
    
    /**
     * Additional error details (optional)
     */
    private Map<String, Object> details;
}

