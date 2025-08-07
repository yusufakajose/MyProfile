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
        
        String targetUrl = linkRedirectService.getRedirectUrl(linkId);
        
        // Publish click event asynchronously (fire-and-forget)
        linkRedirectService.publishClickEvent(linkId, request);
        
        // Return immediate redirect response
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        
        // Add cache headers to allow browser caching for popular links
        headers.add("Cache-Control", "public, max-age=300"); // 5 minutes
        
        log.debug("Redirecting link {} to {}", linkId, targetUrl);
        
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
}
