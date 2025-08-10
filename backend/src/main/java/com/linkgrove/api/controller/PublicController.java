package com.linkgrove.api.controller;

import com.linkgrove.api.dto.PublicProfileResponse;
import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.RateLimitService;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${public.base-url:}")
    private String configuredBaseUrl;

    @Value("${twitter.site:}")
    private String twitterSite;

    @GetMapping("/{username}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable String username, HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) clientIp = request.getRemoteAddr();
        int limit = 120; // 120/min per IP for profile fetches
        java.time.Duration window = java.time.Duration.ofMinutes(1);
        var rl = rateLimitService.checkAndUpdate("pubprofile:" + clientIp, limit, window);
        try {
            PublicProfileResponse profile = linkService.getPublicProfile(username);
            return ResponseEntity.ok()
                    .header("X-RateLimit-Limit", String.valueOf(limit))
                    .header("X-RateLimit-Window", String.valueOf(window.toSeconds()))
                    .header("X-RateLimit-Remaining", String.valueOf(rl.remaining()))
                    .body(profile);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404)
                    .header("X-RateLimit-Limit", String.valueOf(limit))
                    .header("X-RateLimit-Window", String.valueOf(window.toSeconds()))
                    .header("X-RateLimit-Remaining", String.valueOf(Math.max(0, rl.remaining())))
                    .build();
        }
    }

    @GetMapping(value = "/meta/{username}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPublicProfileMeta(@PathVariable String username, HttpServletRequest request) {
        try {
            PublicProfileResponse profile = linkService.getPublicProfile(username);

            String origin = determineOrigin(request);
            String canonicalUrl = origin + "/u/" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);

            String displayName = safe(profile.getDisplayName());
            String titleBase = (displayName != null && !displayName.isBlank()) ? displayName : "@" + username;
            String title = titleBase + " — Linkgrove";
            String description = (profile.getBio() != null && !profile.getBio().isBlank())
                    ? escapeHtml(profile.getBio())
                    : escapeHtml("Discover links shared by " + titleBase + " on Linkgrove.");

            String imageUrl = profile.getProfileImageUrl();
            if (imageUrl != null && !imageUrl.isBlank() && !imageUrl.startsWith("http")) {
                imageUrl = origin + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            if (imageUrl == null || imageUrl.isBlank()) {
                // Use local static fallback served by Nginx: /images/og-default.png
                imageUrl = origin + "/images/og-default.png";
            }
            String cardType = "summary_large_image";

            String ogLocale = normalizeLocale(request.getHeader("Accept-Language"));

            StringBuilder html = new StringBuilder();
            html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escapeHtml(title)).append("</title>")
                .append("<meta property=\"og:type\" content=\"profile\"/>")
                .append("<meta property=\"og:site_name\" content=\"Linkgrove\"/>")
                .append("<meta property=\"og:url\" content=\"").append(canonicalUrl).append("\"/>")
                .append("<meta property=\"og:title\" content=\"").append(escapeHtml(title)).append("\"/>")
                .append("<meta property=\"og:description\" content=\"").append(description).append("\"/>");
            if (ogLocale != null) {
                html.append("<meta property=\\\"og:locale\\\" content=\\\"").append(ogLocale).append("\\\"/>");
            }
            html.append("<meta property=\\\"profile:username\\\" content=\\\"").append(escapeHtml(username)).append("\\\"/>");
            html.append("<meta property=\"og:image\" content=\"").append(escapeHtml(imageUrl)).append("\"/>");
            html.append("<meta property=\\\"og:image:width\\\" content=\\\"1200\\\"/>");
            html.append("<meta property=\\\"og:image:height\\\" content=\\\"630\\\"/>");
            html.append("<meta property=\"og:image:alt\" content=\"").append(escapeHtml(title)).append("\"/>");
            html.append("<meta name=\"twitter:card\" content=\"").append(cardType).append("\"/>")
                .append("<meta name=\"twitter:title\" content=\"").append(escapeHtml(title)).append("\"/>")
                .append("<meta name=\"twitter:description\" content=\"").append(description).append("\"/>");
            html.append("<meta name=\"twitter:image\" content=\"").append(escapeHtml(imageUrl)).append("\"/>");
            if (twitterSite != null && !twitterSite.isBlank()) {
                String site = twitterSite.startsWith("@") ? twitterSite : ("@" + twitterSite);
                html.append("<meta name=\\\"twitter:site\\\" content=\\\"").append(escapeHtml(site)).append("\\\"/>");
            }
            html.append("<link rel=\"canonical\" href=\"").append(canonicalUrl).append("\"/>")
                .append("<meta http-equiv=\"refresh\" content=\"0; url=").append(canonicalUrl).append("\"/>")
                .append("</head><body>")
                .append("<p>Redirecting to <a href=\"").append(canonicalUrl).append("\">")
                .append(canonicalUrl).append("</a></p>")
                .append("</body></html>");

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html.toString());
        } catch (RuntimeException ex) {
            String notFoundHtml = "<!doctype html><html><head><meta charset=\\\"utf-8\\\"><meta name=\\\"robots\\\" content=\\\"noindex\\\"><title>Profile not found — Linkgrove</title></head><body><h1>Profile not found</h1></body></html>";
            return ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_HTML)
                    .body(notFoundHtml);
        }
    }

    private String determineOrigin(HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return configuredBaseUrl.replaceAll("/+$", "");
        }
        String proto = coalesce(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = coalesce(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"));
        if (host == null || host.isBlank()) host = "localhost";
        return proto + "://" + host;
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private String safe(String s) {
        return s == null ? null : s;
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        String out = input;
        out = out.replace("&", "&amp;");
        out = out.replace("<", "&lt;");
        out = out.replace(">", "&gt;");
        out = out.replace("\"", "&quot;");
        out = out.replace("'", "&#39;");
        return out;
    }

    private String normalizeLocale(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null || acceptLanguageHeader.isBlank()) return null;
        String token = acceptLanguageHeader.split(",")[0].trim();
        if (token.isEmpty()) return null;
        // Convert en-US → en_US
        String[] parts = token.split("-");
        if (parts.length == 1) return parts[0].toLowerCase();
        return parts[0].toLowerCase() + "_" + parts[1].toUpperCase();
    }
    @PostMapping("/click/{linkId}")
    public ResponseEntity<Void> trackLinkClick(@PathVariable Long linkId, HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        int limit = 60;
        java.time.Duration window = java.time.Duration.ofMinutes(1);
        var rl = rateLimitService.checkAndUpdate("pubclick:" + clientIp, limit, window);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(rl.retryAfterSeconds()))
                    .header("X-RateLimit-Limit", String.valueOf(limit))
                    .header("X-RateLimit-Window", String.valueOf(window.toSeconds()))
                    .header("X-RateLimit-Remaining", "0")
                    .build();
        }
        linkService.trackLinkClick(linkId, clientIp);
        return ResponseEntity.ok()
                .header("X-RateLimit-Limit", String.valueOf(limit))
                .header("X-RateLimit-Window", String.valueOf(window.toSeconds()))
                .header("X-RateLimit-Remaining", String.valueOf(rl.remaining()))
                .build();
    }
}
