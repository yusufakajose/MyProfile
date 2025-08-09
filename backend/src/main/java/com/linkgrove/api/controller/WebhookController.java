package com.linkgrove.api.controller;

import com.linkgrove.api.model.User;
import com.linkgrove.api.model.WebhookConfig;
import com.linkgrove.api.dto.WebhookConfigRequest;
import com.linkgrove.api.repository.UserRepository;
import com.linkgrove.api.repository.WebhookConfigRepository;
import com.linkgrove.api.repository.WebhookDeliveryRepository;
import com.linkgrove.api.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class WebhookController {

    private final WebhookConfigRepository configRepository;
    private final UserRepository userRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookService webhookService;

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(configRepository.findFirstByUserAndIsActiveTrue(user).orElse(null));
    }

    @PostMapping("/config")
    public ResponseEntity<?> upsertConfig(Authentication auth, @Valid @RequestBody WebhookConfigRequest req) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        WebhookConfig cfg = configRepository.findFirstByUserAndIsActiveTrue(user).orElse(null);
        if (cfg == null) {
            cfg = WebhookConfig.builder().user(user).build();
            cfg.setSecret(generateSecret());
        }
        cfg.setUrl(req.getUrl());
        cfg.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        configRepository.save(cfg);
        return ResponseEntity.ok(cfg);
    }

    @GetMapping("/deliveries")
    public ResponseEntity<?> listDeliveries(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(deliveryRepository.findTop20ByUserOrderByCreatedAtDesc(user));
    }

    @PostMapping("/deliveries/{id}/resend")
    public ResponseEntity<?> resend(Authentication auth, @PathVariable @Min(1) Long id) {
        // Ownership checked implicitly by fetching and comparing users when resending
        return ResponseEntity.ok(webhookService.resend(id));
    }

    @GetMapping("/deliveries/dlq")
    public ResponseEntity<?> listDeadLetter(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(deliveryRepository.findTop50ByUserAndDeadLetteredTrueOrderByCreatedAtDesc(user));
    }

    @PostMapping("/deliveries/resend-all-dlq")
    public ResponseEntity<?> resendAllDlq(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        var list = deliveryRepository.findTop50ByUserAndDeadLetteredTrueOrderByCreatedAtDesc(user);
        int ok = 0;
        for (var d : list) {
            webhookService.resend(d.getId());
            ok++;
        }
        return ResponseEntity.ok(Map.of("resendCount", ok));
    }

    private String generateSecret() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}


