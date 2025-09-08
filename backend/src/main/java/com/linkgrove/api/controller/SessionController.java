package com.linkgrove.api.controller;

import com.linkgrove.api.dto.RefreshSessionResponse;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.RefreshTokenRepository;
import com.linkgrove.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @GetMapping
    public ResponseEntity<List<RefreshSessionResponse>> list(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<RefreshSessionResponse> tokens = refreshTokenRepository.findByUser(user).stream()
                .map(rt -> RefreshSessionResponse.builder()
                        .id(rt.getId())
                        .createdAt(rt.getCreatedAt())
                        .expiresAt(rt.getExpiresAt())
                        .revoked(rt.getRevoked())
                        .ip(rt.getIp())
                        .userAgent(rt.getUserAgent())
                        .lastUsedAt(rt.getLastUsedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(tokens);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(Authentication auth, @PathVariable Long id) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        refreshTokenRepository.findById(id).ifPresent(rt -> {
            if (rt.getUser().getId().equals(user.getId())) {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            }
        });
        return ResponseEntity.noContent().build();
    }
}


