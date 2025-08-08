package com.linkgrove.api.controller;

import com.linkgrove.api.dto.PublicProfileResponse;
import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicController {

    private final LinkService linkService;
    private final RateLimitService rateLimitService;

    @GetMapping("/{username}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable String username) {
        PublicProfileResponse profile = linkService.getPublicProfile(username);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/click/{linkId}")
    public ResponseEntity<Void> trackLinkClick(@PathVariable Long linkId, HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        var rl = rateLimitService.checkAndUpdate("pubclick:" + clientIp, 60, java.time.Duration.ofMinutes(1));
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(rl.retryAfterSeconds()))
                    .header("X-RateLimit-Remaining", "0")
                    .build();
        }
        linkService.trackLinkClick(linkId, clientIp);
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rl.remaining()))
                .build();
    }
}
