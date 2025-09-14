package com.linkgrove.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Generate per-request CSP nonce for style tags (used by frontend frameworks like Emotion/MUI)
        byte[] nonceBytes = new byte[16];
        new SecureRandom().nextBytes(nonceBytes);
        String cspNonce = Base64.getEncoder().encodeToString(nonceBytes);

        // Content Security Policy - remove style-src 'unsafe-inline', allow styles only with matching nonce
        String csp = "default-src 'self'; script-src 'self'; style-src 'self' 'nonce-" + cspNonce + "'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'";
        response.setHeader("Content-Security-Policy", csp);
        // HSTS (only if behind HTTPS)
        String proto = request.getHeader("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(proto) || request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer-when-downgrade");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        filterChain.doFilter(request, response);
    }
}


