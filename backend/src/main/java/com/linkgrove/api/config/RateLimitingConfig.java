package com.linkgrove.api.config;

import com.linkgrove.api.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(1)
public class RateLimitingConfig extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final Map<String, Rule> RULES = new LinkedHashMap<>() {{
        put("/api/public/click/", new Rule(10, 30)); // 30 requests / 10s
        put("/r/", new Rule(10, 30));                // 30 requests / 10s
        put("/api/public/", new Rule(60, 120));      // 120 requests / 60s
    }};

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Rule rule = isQrPath(path) ? new Rule(10, 10) : matchRule(path);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long nowMs = System.currentTimeMillis();
        String clientIp = getClientIp(request);
        String key = "rl:ip:" + clientIp + ":" + rule.prefix;
        long windowMs = rule.windowSeconds * 1000L;
        long cutoff = nowMs - windowMs;

        // Sliding window via ZSET timestamps
        var ops = redisTemplate.opsForZSet();
        ops.removeRangeByScore(key, 0, cutoff);
        Long current = ops.zCard(key);
        if (current != null && current >= rule.maxRequests) {
            throw new RateLimitExceededException("Too many requests", rule.windowSeconds);
        }
        ops.add(key, String.valueOf(nowMs), nowMs);
        redisTemplate.expire(key, Duration.ofSeconds(rule.windowSeconds + 5));

        filterChain.doFilter(request, response);
    }

    private Rule matchRule(String path) {
        for (Map.Entry<String, Rule> e : RULES.entrySet()) {
            if (path.startsWith(e.getKey())) {
                Rule r = e.getValue();
                r.prefix = e.getKey();
                return r;
            }
        }
        return null;
    }

    private boolean isQrPath(String path) {
        // Match expensive QR generation endpoints under /r/{id or a/alias}/qr.{ext}
        return path != null && path.startsWith("/r/") && path.contains("/qr.");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }

    private static class Rule {
        final int windowSeconds;
        final int maxRequests;
        String prefix;
        Rule(int windowSeconds, int maxRequests) {
            this.windowSeconds = windowSeconds;
            this.maxRequests = maxRequests;
        }
    }
}
