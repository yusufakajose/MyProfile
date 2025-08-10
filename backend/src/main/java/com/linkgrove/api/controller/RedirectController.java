package com.linkgrove.api.controller;

import com.linkgrove.api.service.LinkRedirectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * High-performance redirect controller for link clicks.
 * Handles GET /r/{linkId} requests with sub-50ms response times.
 */
@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RedirectController {

    private final LinkRedirectService linkRedirectService;
    private final com.linkgrove.api.service.RateLimitService rateLimitService;
    private final com.linkgrove.api.service.QrCodeService qrCodeService;

    /**
     * Redirect endpoint that handles link clicks with lightning-fast performance.
     * 
     * Flow:
     * 1. Lookup link from cache (Redis) or database
     * 2. Publish click event to RabbitMQ asynchronously 
     * 3. Return 302 redirect immediately (no waiting)
     * 
     * @param linkId The ID of the link to redirect to
     * @param request HTTP request for extracting client info
     * @return 302 redirect response or 404 if link not found
     */
    @GetMapping("/{linkId}")
    public ResponseEntity<Void> redirectToLink(
            @PathVariable Long linkId,
            HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) clientIp = request.getRemoteAddr();
        int limit = 300;
        java.time.Duration window = java.time.Duration.ofMinutes(1);
        var rl = rateLimitService.checkAndUpdate("redir:" + clientIp, limit, window);
        String targetUrl = linkRedirectService.getRedirectUrl(linkId);
        
        // Publish click event asynchronously (fire-and-forget)
        linkRedirectService.publishClickEvent(linkId, request);
        
        // Return immediate redirect response
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        
        // Add cache headers to allow browser caching for popular links
        headers.add("Cache-Control", "public, max-age=300"); // 5 minutes
        
        log.debug("Redirecting link {} to {}", linkId, targetUrl);
        
        if (!rl.allowed()) {
            headers.add("Retry-After", String.valueOf(rl.retryAfterSeconds()));
        }
        headers.add("X-RateLimit-Limit", String.valueOf(limit));
        headers.add("X-RateLimit-Window", String.valueOf(window.toSeconds()));
        headers.add("X-RateLimit-Remaining", String.valueOf(Math.max(0, rl.remaining())));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/a/{alias}")
    public ResponseEntity<Void> redirectToAlias(
            @PathVariable String alias,
            HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) clientIp = request.getRemoteAddr();
        int limit = 300;
        java.time.Duration window = java.time.Duration.ofMinutes(1);
        var rl = rateLimitService.checkAndUpdate("redir:" + clientIp, limit, window);
        var resolved = linkRedirectService.getLinkByAlias(alias);
        String targetUrl = resolved.getUrl();

        // Publish click event using link id
        linkRedirectService.publishClickEvent(resolved.getId(), request);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        headers.add("Cache-Control", "public, max-age=300");
        log.debug("Redirecting alias {} to {} (id {})", alias, targetUrl, resolved.getId());
        if (!rl.allowed()) {
            headers.add("Retry-After", String.valueOf(rl.retryAfterSeconds()));
        }
        headers.add("X-RateLimit-Limit", String.valueOf(limit));
        headers.add("X-RateLimit-Window", String.valueOf(window.toSeconds()));
        headers.add("X-RateLimit-Remaining", String.valueOf(Math.max(0, rl.remaining())));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * Preview endpoint that returns link information without redirecting.
     * Useful for link validation and preview generation.
     * 
     * @param linkId The ID of the link to preview
     * @return Link preview information
     */
    @GetMapping("/{linkId}/preview")
    public ResponseEntity<?> previewLink(@PathVariable Long linkId) {
        var preview = linkRedirectService.getLinkPreview(linkId);
        return ResponseEntity.ok(preview);
    }

    @GetMapping(value = "/{linkId}/qr.png", produces = "image/png")
    public ResponseEntity<byte[]> qrPng(@PathVariable Long linkId,
                                        @RequestParam(required = false, defaultValue = "256") int size,
                                        @RequestParam(required = false, defaultValue = "1") int margin,
                                        @RequestParam(required = false) String utm,
                                        @RequestParam(required = false) String fg,
                                        @RequestParam(required = false) String bg,
                                        @RequestParam(required = false) String logo,
                                        @RequestParam(required = false, name = "ecc") String ecc,
                                        HttpServletRequest request) {
        if (logo != null && !logo.isBlank() && !isAllowedLogoUrl(logo)) {
            throw new IllegalArgumentException("Invalid logo URL. Only https and png/jpg/jpeg are allowed.");
        }
        String url = buildShortUrl(request, "/r/" + linkId);
        url = maybeAppendUtm(url, utm);
        Integer fgArgb = parseHexColor(fg);
        Integer bgArgb = parseHexColor(bg);
        validateContrastOrThrow(fgArgb, bgArgb);
        ErrorCorrectionLevel lvl = parseEcc(ecc);
        byte[] png = qrCodeService.generatePng(url, clamp(size, 128, 1024), clamp(margin, 0, 4), fgArgb, bgArgb, logo, lvl);
        String etag = computeEtag(png);
        String inm = request.getHeader("If-None-Match");
        if (etag.equals(inm)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("ETag", etag)
                    .header("Cache-Control", "public, max-age=86400, immutable")
                    .header("X-RateLimit-Limit", "60")
                    .header("X-RateLimit-Window", String.valueOf(60))
                    .header("X-RateLimit-Policy", "ip; window=60; max=60")
                    .header("Content-Disposition", "inline; filename=\"qr-" + linkId + ".png\"")
                    .build();
        }
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=86400, immutable")
                .header("X-RateLimit-Limit", "60")
                .header("X-RateLimit-Window", String.valueOf(60))
                .header("X-RateLimit-Policy", "ip; window=60; max=60")
                .header("Content-Disposition", "inline; filename=\"qr-" + linkId + ".png\"")
                .body(png);
    }

    @GetMapping(value = "/{linkId}/qr.svg", produces = "image/svg+xml")
    public ResponseEntity<String> qrSvg(@PathVariable Long linkId,
                                        @RequestParam(required = false, defaultValue = "256") int size,
                                        @RequestParam(required = false, defaultValue = "1") int margin,
                                        @RequestParam(required = false) String utm,
                                        @RequestParam(required = false) String fg,
                                        @RequestParam(required = false) String bg,
                                        @RequestParam(required = false, name = "ecc") String ecc,
                                        HttpServletRequest request) {
        String url = buildShortUrl(request, "/r/" + linkId);
        url = maybeAppendUtm(url, utm);
        Integer fgArgb = parseHexColor(fg);
        Integer bgArgb = parseHexColor(bg);
        validateContrastOrThrow(fgArgb, bgArgb);
        ErrorCorrectionLevel lvl = parseEcc(ecc);
        String svg = qrCodeService.generateSvg(url, clamp(size, 128, 1024), clamp(margin, 0, 4), fgArgb, bgArgb, lvl);
        String etag = computeEtag(svg.getBytes(StandardCharsets.UTF_8));
        String inm = request.getHeader("If-None-Match");
        if (etag.equals(inm)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("ETag", etag)
                    .header("Cache-Control", "public, max-age=86400, immutable")
                    .header("X-RateLimit-Limit", "60")
                    .header("X-RateLimit-Window", String.valueOf(60))
                    .header("X-RateLimit-Policy", "ip; window=60; max=60")
                    .header("Content-Disposition", "inline; filename=\"qr-" + linkId + ".svg\"")
                    .build();
        }
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=86400, immutable")
                .header("X-RateLimit-Limit", "60")
                .header("X-RateLimit-Window", String.valueOf(60))
                .header("X-RateLimit-Policy", "ip; window=60; max=60")
                .header("Content-Disposition", "inline; filename=\"qr-" + linkId + ".svg\"")
                .body(svg);
    }

    @GetMapping(value = "/a/{alias}/qr.png", produces = "image/png")
    public ResponseEntity<byte[]> qrAliasPng(@PathVariable String alias,
                                             @RequestParam(required = false, defaultValue = "256") int size,
                                             @RequestParam(required = false, defaultValue = "1") int margin,
                                             @RequestParam(required = false) String utm,
                                             @RequestParam(required = false) String fg,
                                             @RequestParam(required = false) String bg,
                                             @RequestParam(required = false) String logo,
                                              @RequestParam(required = false, name = "ecc") String ecc,
                                             HttpServletRequest request) {
        if (logo != null && !logo.isBlank() && !isAllowedLogoUrl(logo)) {
            throw new IllegalArgumentException("Invalid logo URL. Only https and png/jpg/jpeg are allowed.");
        }
        String url = buildShortUrl(request, "/r/a/" + alias);
        url = maybeAppendUtm(url, utm);
        Integer fgArgb = parseHexColor(fg);
        Integer bgArgb = parseHexColor(bg);
        validateContrastOrThrow(fgArgb, bgArgb);
        ErrorCorrectionLevel lvl = parseEcc(ecc);
        byte[] png = qrCodeService.generatePng(url, clamp(size, 128, 1024), clamp(margin, 0, 4), fgArgb, bgArgb, logo, lvl);
        String etag = computeEtag(png);
        String inm = request.getHeader("If-None-Match");
        if (etag.equals(inm)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("ETag", etag)
                    .header("Cache-Control", "public, max-age=86400, immutable")
                    .header("X-RateLimit-Limit", "60")
                    .header("X-RateLimit-Window", String.valueOf(60))
                    .header("X-RateLimit-Policy", "ip; window=60; max=60")
                    .header("Content-Disposition", "inline; filename=\"qr-" + alias + ".png\"")
                    .build();
        }
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=86400, immutable")
                .header("X-RateLimit-Limit", "60")
                .header("X-RateLimit-Window", String.valueOf(60))
                .header("X-RateLimit-Policy", "ip; window=60; max=60")
                .header("Content-Disposition", "inline; filename=\"qr-" + alias + ".png\"")
                .body(png);
    }

    @GetMapping(value = "/a/{alias}/qr.svg", produces = "image/svg+xml")
    public ResponseEntity<String> qrAliasSvg(@PathVariable String alias,
                                             @RequestParam(required = false, defaultValue = "256") int size,
                                             @RequestParam(required = false, defaultValue = "1") int margin,
                                             @RequestParam(required = false) String utm,
                                             @RequestParam(required = false) String fg,
                                             @RequestParam(required = false) String bg,
                                              @RequestParam(required = false, name = "ecc") String ecc,
                                             HttpServletRequest request) {
        String url = buildShortUrl(request, "/r/a/" + alias);
        url = maybeAppendUtm(url, utm);
        Integer fgArgb = parseHexColor(fg);
        Integer bgArgb = parseHexColor(bg);
        validateContrastOrThrow(fgArgb, bgArgb);
        ErrorCorrectionLevel lvl = parseEcc(ecc);
        String svg = qrCodeService.generateSvg(url, clamp(size, 128, 1024), clamp(margin, 0, 4), fgArgb, bgArgb, lvl);
        String etag = computeEtag(svg.getBytes(StandardCharsets.UTF_8));
        String inm = request.getHeader("If-None-Match");
        if (etag.equals(inm)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("ETag", etag)
                    .header("Cache-Control", "public, max-age=86400, immutable")
                    .header("X-RateLimit-Limit", "60")
                    .header("X-RateLimit-Window", String.valueOf(60))
                    .header("X-RateLimit-Policy", "ip; window=60; max=60")
                    .header("Content-Disposition", "inline; filename=\"qr-" + alias + ".svg\"")
                    .build();
        }
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=86400, immutable")
                .header("X-RateLimit-Limit", "60")
                .header("X-RateLimit-Window", String.valueOf(60))
                .header("X-RateLimit-Policy", "ip; window=60; max=60")
                .header("Content-Disposition", "inline; filename=\"qr-" + alias + ".svg\"")
                .body(svg);
    }

    @PostMapping(value = "/{linkId}/qr/pregenerate")
    public ResponseEntity<Void> pregenerateQrForLink(@PathVariable Long linkId) {
        // Pre-generate a few common presets to warm caches/CDN
        int[] sizes = {256, 512};
        String[] eccs = {"M", "H"};
        for (int s : sizes) {
            for (String e : eccs) {
                // Warm generation paths; rely on HTTP/CDN caches when requested
                qrCodeService.generateSvg("/r/" + linkId, s, 1, null, null, parseEcc(e));
            }
        }
        return ResponseEntity.accepted().build();
    }

    private String buildShortUrl(HttpServletRequest request, String path) {
        String proto = coalesce(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = coalesce(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"));
        if (host == null || host.isBlank()) host = "localhost:8080";
        if (!path.startsWith("/")) path = "/" + path;
        return proto + "://" + host + path;
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String maybeAppendUtm(String url, String utm) {
        if (utm == null || utm.isBlank()) return url;
        try {
            java.net.URI uri = java.net.URI.create(url);
            String q = uri.getQuery();
            String extra = "utm_medium=qr&utm_source=linkgrove&src=qr";
            String newQuery = (q == null || q.isBlank()) ? extra : (q + "&" + extra);
            return new java.net.URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment()).toString();
        } catch (Exception e) {
            return url;
        }
    }

    private Integer parseHexColor(String hex) {
        if (hex == null || hex.isBlank()) return null;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        try {
            int rgb = (int) Long.parseLong(s, 16);
            return 0xFF000000 | rgb; // add opaque alpha
        } catch (Exception e) {
            return null;
        }
    }

    private ErrorCorrectionLevel parseEcc(String ecc) {
        if (ecc == null) return ErrorCorrectionLevel.M;
        String v = ecc.trim().toUpperCase();
        return switch (v) {
            case "L" -> ErrorCorrectionLevel.L;
            case "M" -> ErrorCorrectionLevel.M;
            case "Q" -> ErrorCorrectionLevel.Q;
            case "H" -> ErrorCorrectionLevel.H;
            default -> ErrorCorrectionLevel.M;
        };
    }

    private String computeEtag(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(2 + digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            // Strong ETag with quotes
            return '"' + sb.toString() + '"';
        } catch (Exception e) {
            // Fallback weak ETag
            return '"' + Integer.toHexString(java.util.Arrays.hashCode(data)) + '"';
        }
    }

    @SuppressWarnings("unused")
    private boolean hasSufficientContrast(Integer fgArgb, Integer bgArgb) {
        if (fgArgb == null && bgArgb == null) return true; // no custom colors provided
        int fgEff = (fgArgb != null ? fgArgb : 0xFF000000); // default black
        int bgEff = (bgArgb != null ? bgArgb : 0xFFFFFFFF); // default white
        return contrastRatio(fgEff, bgEff) >= 2.5; // align with service threshold
    }

    private void validateContrastOrThrow(Integer fgArgb, Integer bgArgb) {
        if (fgArgb == null && bgArgb == null) return; // nothing to validate
        int fgEff = (fgArgb != null ? fgArgb : 0xFF000000);
        int bgEff = (bgArgb != null ? bgArgb : 0xFFFFFFFF);
        double c = contrastRatio(fgEff, bgEff);
        if (c < 2.5) {
            String msg = String.format("Insufficient contrast between fg and bg (ratio %.2f). Provide higher contrast (>= 2.5) using dark fg and light bg.", c);
            throw new IllegalArgumentException(msg);
        }
    }

    private double contrastRatio(int argb1, int argb2) {
        double l1 = relativeLuminance(argb1);
        double l2 = relativeLuminance(argb2);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double relativeLuminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        double rs = srgbToLinear(r / 255.0);
        double gs = srgbToLinear(g / 255.0);
        double bs = srgbToLinear(b / 255.0);
        return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
    }

    private double srgbToLinear(double c) {
        return (c <= 0.03928) ? (c / 12.92) : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private boolean isAllowedLogoUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            java.net.URL u = uri.toURL();
            if (!"https".equalsIgnoreCase(u.getProtocol())) return false;
            String p = u.getPath().toLowerCase();
            return p.endsWith(".png") || p.endsWith(".jpg") || p.endsWith(".jpeg");
        } catch (Exception e) {
            return false;
        }
    }

    // Kept for potential future use (JSON body from within controller)
    @SuppressWarnings("unused")
    private ResponseEntity<byte[]> badRequestJson(String message) {
        String json = "{\"timestamp\":\"" + java.time.Instant.now() +
                "\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"" +
                escapeJson(message) + "\"}";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
