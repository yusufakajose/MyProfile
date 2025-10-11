package com.linkgrove.api.exception;

/**
 * Exception thrown when attempting to create or update a link with an alias that already exists.
 */
public class AliasAlreadyExistsException extends RuntimeException {
    
    public AliasAlreadyExistsException(String alias) {
        super("Alias already in use: " + alias);
    }
}

