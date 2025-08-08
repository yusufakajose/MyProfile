package com.linkgrove.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("admin ok");
    }

    @GetMapping("/metrics/ratelimits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRateLimitMetrics() {
        Map<String, Object> out = new HashMap<>();
        Map<String, Long> prefixes = new LinkedHashMap<>();
        prefixes.put("redir", 0L);
        prefixes.put("pubclick", 0L);
        for (String prefix : prefixes.keySet()) {
            long total = 0;
            Set<String> keys = redisTemplate.keys("rl:z:" + prefix + "*");
            if (keys != null) {
                for (String k : keys) {
                    Long z = redisTemplate.opsForZSet().zCard(k);
                    total += (z == null ? 0 : z);
                }
            }
            prefixes.put(prefix, total);
        }
        out.put("rateLimitTotals", prefixes);
        out.put("keysCount", Optional.ofNullable(redisTemplate.keys("rl:z:*")).map(Set::size).orElse(0));
        return ResponseEntity.ok(out);
    }
}
