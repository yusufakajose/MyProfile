package com.linkgrove.api.controller;

import com.linkgrove.api.model.User;
import com.linkgrove.api.model.WebhookConfig;
import com.linkgrove.api.repository.UserRepository;
import com.linkgrove.api.repository.WebhookConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebhookController {

    private final WebhookConfigRepository configRepository;
    private final UserRepository userRepository;

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(configRepository.findFirstByUserAndIsActiveTrue(user).orElse(null));
    }

    @PostMapping("/config")
    public ResponseEntity<?> upsertConfig(Authentication auth, @RequestBody WebhookConfig req) {
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

    private String generateSecret() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}


