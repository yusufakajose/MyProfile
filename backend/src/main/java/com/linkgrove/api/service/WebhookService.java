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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookConfigRepository configRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional(readOnly = true)
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

        String signature = hmacSha256(cfg.getSecret(), json);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Webhook-Signature", signature);
        headers.add("X-Webhook-Event", eventType);
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
                .build();
        deliveryRepository.save(d);
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
}


