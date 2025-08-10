package com.linkgrove.api.exception;

/**
 * Exception thrown when a requested link is not found or inactive.
 */
public class LinkNotFoundException extends RuntimeException {
    
    public LinkNotFoundException(String message) {
        super(message);
    }
    
    public LinkNotFoundException(Long linkId) {
        super("Link not found with ID: " + linkId);
    }
    
    public LinkNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

