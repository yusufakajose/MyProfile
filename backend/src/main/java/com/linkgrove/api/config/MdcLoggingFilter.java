package com.linkgrove.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(3)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String MDC_METHOD = "httpMethod";
    public static final String MDC_PATH = "httpPath";
    public static final String MDC_QUERY = "httpQuery";
    public static final String MDC_STATUS = "httpStatus";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_USERNAME = "username";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        // Basic request context
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_PATH, request.getRequestURI());
        String q = request.getQueryString();
        if (q != null && !q.isBlank()) {
            MDC.put(MDC_QUERY, q);
        }
        String clientIp = extractClientIp(request);
        if (clientIp != null) {
            MDC.put(MDC_CLIENT_IP, clientIp);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put(MDC_STATUS, String.valueOf(response.getStatus()));
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put(MDC_USERNAME, String.valueOf(auth.getName()));
            }
            // Do not remove here to allow appenders to read; remove at request end
            MDC.remove(MDC_QUERY);
            // Keep other fields for encoders reading after chain; they will be overwritten on next request
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}


