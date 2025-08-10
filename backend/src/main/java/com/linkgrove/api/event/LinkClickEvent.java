package com.linkgrove.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event representing a link click for asynchronous processing.
 * This event is published to RabbitMQ when a user clicks on a tracked link.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkClickEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * The ID of the link that was clicked
     */
    private Long linkId;
    
    /**
     * The username of the link owner
     */
    private String username;
    
    /**
     * Timestamp when the click occurred
     */
    private Instant clickedAt;
    
    /**
     * Client IP address (for analytics and fraud detection)
     */
    private String clientIp;
    
    /**
     * User Agent string (for analytics)
     */
    private String userAgent;
    
    /**
     * Referring URL (optional)
     */
    private String referrer;
    
    /**
     * Session ID for tracking unique visitors (optional)
     */
    private String sessionId;
    
    /**
     * The target URL that the user was redirected to
     */
    private String targetUrl;

    /**
     * Selected variant id if A/B variant was used (nullable)
     */
    private Long variantId;

    // Optional UTM/source parameters extracted from the inbound short-link request
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmTerm;
    private String utmContent;
    private String source; // e.g., "qr"
}
