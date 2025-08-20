package com.linkgrove.api.service;

import com.linkgrove.api.model.User;
import com.linkgrove.api.model.WebhookConfig;
import com.linkgrove.api.model.WebhookDelivery;
import com.linkgrove.api.repository.UserRepository;
import com.linkgrove.api.repository.WebhookConfigRepository;
import com.linkgrove.api.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.MDC;
import com.linkgrove.api.config.RequestIdFilter;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookConfigRepository configRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${webhooks.maxRetriesPerDestinationPerDay:100}")
    private int maxRetriesPerDestinationPerDay;

    @Transactional
    public void emitLinkClick(String username, Long linkId, String url, String referrer, String ip, String userAgent) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;
        WebhookConfig cfg = configRepository.findFirstByUserAndIsActiveTrue(user).orElse(null);
        if (cfg == null) return;

        String eventType = "link.click";
        Map<String, Object> payload = Map.of(
                "type", eventType,
                "username", username,
                "linkId", linkId,
                "url", url,
                "referrer", referrer,
                "ip", ip,
                "userAgent", userAgent,
                "occurredAt", java.time.Instant.now().toString()
        );

        String json;
        try {
            json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize webhook payload: {}", e.getMessage());
            return;
        }

        long ts = Instant.now().getEpochSecond();
        String nonce = generateNonce(16);
        String signatureBase = ts + "." + nonce + "." + json;
        String signature = hmacSha256(cfg.getSecret(), signatureBase);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Webhook-Signature", signature);
        headers.add("X-Webhook-Signature-Alg", "HMAC-SHA256");
        headers.add("X-Webhook-Signature-Version", "v1");
        headers.add("X-Webhook-Timestamp", String.valueOf(ts));
        headers.add("X-Webhook-Nonce", nonce);
        headers.add("X-Webhook-Event", eventType);
        // Propagate request id to downstream webhooks if present
        String rid = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
        if (rid != null && !rid.isBlank()) {
            headers.add(RequestIdFilter.HEADER_REQUEST_ID, rid);
        }
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        int status = 0;
        String error = null;
        try {
            var resp = restTemplate.postForEntity(cfg.getUrl(), entity, String.class);
            status = resp.getStatusCode().value();
        } catch (Exception e) {
            status = 0;
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        WebhookDelivery d = WebhookDelivery.builder()
                .user(user)
                .eventType(eventType)
                .targetUrl(cfg.getUrl())
                .attempt(1)
                .statusCode(status)
                .createdAt(LocalDateTime.now())
                .errorMessage(error != null && error.length() > 480 ? error.substring(0, 480) : error)
                .payload(json)
                .deadLettered(false)
                .nextAttemptAt(computeNextAttemptAt(status, 1))
                .build();
        // If scheduling a retry, enforce per-destination/day cap
        if (d.getNextAttemptAt() != null) {
            if (incrementAndCheckDestinationRetryCap(cfg.getUrl())) {
                d.setDeadLettered(true);
                d.setNextAttemptAt(null);
            }
        }
        deliveryRepository.save(d);
    }

    @Transactional
    public WebhookDelivery resend(Long deliveryId) {
        WebhookDelivery d = deliveryRepository.findById(deliveryId).orElseThrow();
        User user = d.getUser();
        WebhookConfig cfg = configRepository.findFirstByUserAndIsActiveTrue(user).orElse(null);
        if (cfg == null) return d;
        String json = d.getPayload();
        long ts = Instant.now().getEpochSecond();
        String nonce = generateNonce(16);
        String signatureBase = ts + "." + nonce + "." + json;
        String signature = hmacSha256(cfg.getSecret(), signatureBase);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Webhook-Signature", signature);
        headers.add("X-Webhook-Signature-Alg", "HMAC-SHA256");
        headers.add("X-Webhook-Signature-Version", "v1");
        headers.add("X-Webhook-Timestamp", String.valueOf(ts));
        headers.add("X-Webhook-Nonce", nonce);
        headers.add("X-Webhook-Event", d.getEventType());
        String rid = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
        if (rid != null && !rid.isBlank()) {
            headers.add(RequestIdFilter.HEADER_REQUEST_ID, rid);
        }
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        int status = 0; String error = null;
        try {
            var resp = restTemplate.postForEntity(cfg.getUrl(), entity, String.class);
            status = resp.getStatusCode().value();
        } catch (Exception e) {
            status = 0;
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        d.setAttempt(d.getAttempt() + 1);
        d.setStatusCode(status);
        d.setErrorMessage(error != null && error.length() > 480 ? error.substring(0, 480) : error);
        int attempt = d.getAttempt() != null ? d.getAttempt() : 1;
        d.setDeadLettered(shouldDeadLetter(status, attempt));
        d.setNextAttemptAt(d.getDeadLettered() ? null : computeNextAttemptAt(status, attempt));
        if (!d.getDeadLettered() && d.getNextAttemptAt() != null) {
            if (incrementAndCheckDestinationRetryCap(cfg.getUrl())) {
                d.setDeadLettered(true);
                d.setNextAttemptAt(null);
            }
        }
        return deliveryRepository.save(d);
    }

    private String hmacSha256(String secret, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String generateNonce(int numBytes) {
        byte[] buf = new byte[Math.max(8, numBytes)];
        SECURE_RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private LocalDateTime computeNextAttemptAt(int statusCode, int attempt) {
        // Only retry on network errors (0) and 5xx
        boolean retryable = statusCode == 0 || (statusCode >= 500 && statusCode < 600);
        if (!retryable) return null;
        long baseSeconds = 15; // start 15s, then 30s, 60s, 120s, capped
        long delay = baseSeconds * (1L << Math.min(5, Math.max(0, attempt - 1))); // cap shift at 5
        if (delay > 1800) delay = 1800; // cap 30m
        // add jitter +/- 20%
        double jitter = 0.8 + (SECURE_RANDOM.nextDouble() * 0.4);
        long jittered = Math.max(5, Math.round(delay * jitter));
        return LocalDateTime.now().plusSeconds(jittered);
    }

    private boolean shouldDeadLetter(int statusCode, int attempt) {
        if (!(statusCode == 0 || (statusCode >= 500 && statusCode < 600))) {
            return false; // non-retryable, but considered final success/failure without DLQ
        }
        int maxAttempts = 6; // cap attempts per delivery
        return attempt >= maxAttempts;
    }

    private boolean incrementAndCheckDestinationRetryCap(String url) {
        try {
            String host;
            try {
                java.net.URI uri = java.net.URI.create(url);
                host = uri.getHost();
                if (host == null || host.isBlank()) host = "unknown";
            } catch (Exception e) {
                host = "unknown";
            }
            String day = java.time.LocalDate.now().toString(); // ISO yyyy-MM-dd
            String key = String.format("wh:dest:%s:%s", host, day);
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                // Set TTL ~2 days to allow observation window
                stringRedisTemplate.expire(key, java.time.Duration.ofDays(2));
            }
            long c = count == null ? 0L : count;
            return c > Math.max(1, maxRetriesPerDestinationPerDay);
        } catch (Exception e) {
            // Fail-open: do not block retries if Redis unavailable
            return false;
        }
    }
}


