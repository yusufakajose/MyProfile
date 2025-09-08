package com.linkgrove.api.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.previous-secrets:}")
    private String previousSecretsCsv;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey[] getVerificationKeys() {
        java.util.List<SecretKey> keys = new java.util.ArrayList<>();
        keys.add(getSigningKey());
        if (previousSecretsCsv != null && !previousSecretsCsv.isBlank()) {
            for (String s : previousSecretsCsv.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    keys.add(Keys.hmacShaKeyFor(trimmed.getBytes()));
                }
            }
        }
        return keys.toArray(new SecretKey[0]);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Try current key, then fall back to previous secrets for rotation window
        for (SecretKey key : getVerificationKeys()) {
            try {
                return Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception ignored) {
            }
        }
        throw new io.jsonwebtoken.security.SignatureException("JWT signature verification failed");
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
