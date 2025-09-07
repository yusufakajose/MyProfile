package com.linkgrove.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LockoutService {

    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    public boolean isLocked(String usernameOrIp) {
        String key = key(usernameOrIp);
        String val = redis.opsForValue().get(key);
        if (val == null) return false;
        try { return Integer.parseInt(val) >= 5; } catch (NumberFormatException e) { return false; }
    }

    public void onFailedAttempt(String usernameOrIp) {
        String key = key(usernameOrIp);
        Long c = redis.opsForValue().increment(key);
        if (c != null && c == 1L) {
            redis.expire(key, Duration.ofMinutes(15));
        }
    }

    public void onSuccess(String usernameOrIp) {
        redis.delete(key(usernameOrIp));
    }

    private String key(String u) { return "lock:" + u; }
}


