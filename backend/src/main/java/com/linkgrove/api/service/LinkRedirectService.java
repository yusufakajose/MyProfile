package com.linkgrove.api.service;

import com.linkgrove.api.config.RabbitMQConfig;
import com.linkgrove.api.event.LinkClickEvent;
import com.linkgrove.api.exception.LinkNotFoundException;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance service for handling link redirects and click tracking.
 * Optimized for sub-50ms response times with Redis caching and async processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkRedirectService {

    private final LinkRepository linkRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Get redirect URL for a link with Redis caching for performance.
     * Cache TTL: 15 minutes (links don't change often)
     * 
     * @param linkId The link ID to redirect to
     * @return The target URL
     * @throws RuntimeException if link not found or inactive
     */
    @Cacheable(value = "linkRedirects", key = "#linkId")
    @Transactional(readOnly = true)
    public String getRedirectUrl(Long linkId) {
        log.debug("Fetching redirect URL from database for link: {}", linkId);
        
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new LinkNotFoundException(linkId));
        
        if (!link.getIsActive()) {
            throw new LinkNotFoundException("Link is inactive: " + linkId);
        }
        
        return link.getUrl();
    }

    @Cacheable(value = "linkRedirectsByAlias", key = "#alias")
    @Transactional(readOnly = true)
    public Link getLinkByAlias(String alias) {
        Link link = linkRepository.findByAlias(alias)
                .orElseThrow(() -> new LinkNotFoundException("Alias not found: " + alias));
        if (!link.getIsActive()) {
            throw new LinkNotFoundException("Link is inactive for alias: " + alias);
        }
        return link;
    }

    /**
     * Publish click event to RabbitMQ for asynchronous processing.
     * This method is fire-and-forget for maximum redirect performance.
     * 
     * @param linkId The link that was clicked
     * @param request HTTP request for extracting client information
     */
    public void publishClickEvent(Long linkId, HttpServletRequest request) {
        try {
            // Extract client information for analytics
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String referrer = request.getHeader("Referer");
            String sessionId = request.getSession(false) != null ? 
                request.getSession().getId() : null;

            // Get the target URL for the event (from cache if possible)
            String targetUrl = getRedirectUrl(linkId);
            
            // Get username from link (cached)
            String username = getLinkOwnerUsername(linkId);

            // Create click event
            LinkClickEvent event = LinkClickEvent.builder()
                    .linkId(linkId)
                    .username(username)
                    .clickedAt(Instant.now())
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .sessionId(sessionId)
                    .targetUrl(targetUrl)
                    .build();

            // Publish to RabbitMQ asynchronously
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.LINK_CLICK_EXCHANGE,
                RabbitMQConfig.LINK_CLICK_ROUTING_KEY,
                event
            );
            
            log.debug("Published click event for link: {}", linkId);
            
        } catch (Exception e) {
            // Don't let analytics failures affect redirects
            log.error("Failed to publish click event for link {}: {}", linkId, e.getMessage());
        }
    }

    /**
     * Get link preview information without redirecting.
     * Useful for link validation and preview cards.
     * 
     * @param linkId The link ID to preview
     * @return Preview information map
     */
    @Cacheable(value = "linkPreviews", key = "#linkId")
    @Transactional(readOnly = true)
    public Map<String, Object> getLinkPreview(Long linkId) {
        log.debug("Fetching link preview for: {}", linkId);
        
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));
        
        Map<String, Object> preview = new HashMap<>();
        preview.put("id", link.getId());
        preview.put("title", link.getTitle());
        preview.put("url", link.getUrl());
        preview.put("description", link.getDescription());
        preview.put("isActive", link.getIsActive());
        preview.put("clickCount", link.getClickCount());
        preview.put("owner", link.getUser().getUsername());
        
        return preview;
    }

    /**
     * Get the username of the link owner (cached for performance).
     * 
     * @param linkId The link ID
     * @return Username of the link owner
     */
    @Cacheable(value = "linkOwners", key = "#linkId")
    @Transactional(readOnly = true)
    public String getLinkOwnerUsername(Long linkId) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new LinkNotFoundException(linkId));
        return link.getUser().getUsername();
    }

    /**
     * Extract client IP address from request, handling proxies and load balancers.
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
