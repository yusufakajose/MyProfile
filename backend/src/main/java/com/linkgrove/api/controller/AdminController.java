package com.linkgrove.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.linkgrove.api.repository.WebhookDeliveryRepository;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final WebhookDeliveryRepository webhookDeliveryRepository;

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
        // Webhook per-destination/day retry counters
        Map<String, String> whSample = new LinkedHashMap<>();
        Set<String> whKeys = redisTemplate.keys("wh:dest:*:*");
        if (whKeys != null) {
            int sample = 0;
            for (String k : whKeys) {
                if (sample++ >= 20) break; // limit payload
                String v = redisTemplate.opsForValue().get(k);
                whSample.put(k, v == null ? "0" : v);
            }
        }
        out.put("webhookDestinationRetryCountsSample", whSample);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/metrics/webhooks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getWebhookMetrics() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", webhookDeliveryRepository.countAll());
        out.put("success", webhookDeliveryRepository.countSuccess());
        out.put("deadLettered", webhookDeliveryRepository.countDeadLettered());
        // top destinations (limit 20)
        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] row : webhookDeliveryRepository.countByTargetUrl()) {
            if (top.size() >= 20) break;
            String targetUrl = String.valueOf(row[0]);
            long count = Long.parseLong(String.valueOf(row[1]));
            top.add(Map.of("targetUrl", targetUrl, "count", count));
        }
        out.put("topDestinations", top);

        // last 24h window
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusHours(24);
        Map<String, Object> last24h = new LinkedHashMap<>();
        last24h.put("total", webhookDeliveryRepository.countAllSince(since));
        last24h.put("success", webhookDeliveryRepository.countSuccessSince(since));
        last24h.put("deadLettered", webhookDeliveryRepository.countDeadLetteredSince(since));

        List<Map<String, Object>> dlqTopAll = new ArrayList<>();
        for (Object[] row : webhookDeliveryRepository.countDeadLetteredByTargetUrl()) {
            if (dlqTopAll.size() >= 20) break;
            dlqTopAll.add(Map.of(
                    "targetUrl", String.valueOf(row[0]),
                    "dlqCount", Long.parseLong(String.valueOf(row[1]))
            ));
        }
        List<Map<String, Object>> dlqTop24h = new ArrayList<>();
        for (Object[] row : webhookDeliveryRepository.countDeadLetteredByTargetUrlSince(since)) {
            if (dlqTop24h.size() >= 20) break;
            dlqTop24h.add(Map.of(
                    "targetUrl", String.valueOf(row[0]),
                    "dlqCount", Long.parseLong(String.valueOf(row[1]))
            ));
        }
        last24h.put("dlqTopDestinations", dlqTop24h);
        out.put("dlqTopDestinationsAll", dlqTopAll);
        out.put("last24h", last24h);
        return ResponseEntity.ok(out);
    }
}
