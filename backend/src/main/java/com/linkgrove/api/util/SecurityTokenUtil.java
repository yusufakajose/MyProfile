package com.linkgrove.api.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utility for generating cryptographically secure random tokens.
 * Centralizes token generation logic to ensure consistency across the application.
 */
@Component
public class SecurityTokenUtil {

    private static final int TOKEN_BYTES = 32; // 256 bits = 64 hex chars
    private final SecureRandom secureRandom;

    public SecurityTokenUtil() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a cryptographically secure random token.
     *
     * @return A 64-character hexadecimal string (32 bytes)
     */
    public String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

