package com.linkgrove.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public RateLimitResult checkAndUpdate(String key, int limit, java.time.Duration window) {
        long nowMs = System.currentTimeMillis();
        String zsetKey = "rl:z:" + key;
        long windowStart = nowMs - window.toMillis();
        // remove old
        redisTemplate.opsForZSet().removeRangeByScore(zsetKey, 0, windowStart);
        // add current
        redisTemplate.opsForZSet().add(zsetKey, String.valueOf(nowMs), nowMs);
        Long count = redisTemplate.opsForZSet().zCard(zsetKey);
        redisTemplate.expire(zsetKey, window.plusSeconds(10));
        boolean allowed = count != null && count <= limit;
        long remaining = Math.max(0, limit - (count == null ? 0 : count));
        // compute retryAfter based on earliest entry
        java.util.Set<String> windowMembers = redisTemplate.opsForZSet().rangeByScore(zsetKey, (double) windowStart, (double) nowMs);
        Double oldest = null;
        if (windowMembers != null && !windowMembers.isEmpty()) {
            String first = windowMembers.iterator().next();
            try { oldest = Double.valueOf(first); } catch (NumberFormatException ignored) {}
        }
        long retryAfterMs = 0;
        if (!allowed && oldest != null) {
            retryAfterMs = (long) (oldest + window.toMillis() - nowMs);
            if (retryAfterMs < 0) retryAfterMs = 0;
        }
        return new RateLimitResult(allowed, remaining, (int) Math.ceil(retryAfterMs / 1000.0));
    }

    public record RateLimitResult(boolean allowed, long remaining, int retryAfterSeconds) {}
}


