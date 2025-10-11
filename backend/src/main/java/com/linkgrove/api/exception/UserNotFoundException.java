package com.linkgrove.api.exception;

/**
 * Exception thrown when a user cannot be found by username or ID.
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
    
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
}

