package com.linkgrove.api.worker;

import com.linkgrove.api.config.RabbitMQConfig;
import com.linkgrove.api.event.LinkClickEvent;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.LinkClickDailyAggregateRepository;
import com.linkgrove.api.repository.LinkReferrerDailyAggregateRepository;
import com.linkgrove.api.repository.LinkDeviceDailyAggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;



/**
 * Asynchronous worker that processes link click events from RabbitMQ.
 * Handles analytics aggregation, click counting, and cache invalidation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsWorker {

    private final LinkRepository linkRepository;
    private final LinkClickDailyAggregateRepository aggregateRepository;
    private final LinkReferrerDailyAggregateRepository referrerAggregateRepository;
    private final LinkDeviceDailyAggregateRepository deviceAggregateRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.linkgrove.api.service.WebhookService webhookService;
    private final com.linkgrove.api.repository.LinkVariantDailyAggregateRepository variantAggregateRepository;
    private final com.linkgrove.api.repository.LinkGeoDailyAggregateRepository geoAggregateRepository;
    private final com.linkgrove.api.service.GeoIpService geoIpService;
    private final com.linkgrove.api.repository.LinkSourceDailyAggregateRepository sourceAggregateRepository;

    /**
     * Process link click events from RabbitMQ queue.
     * This method runs asynchronously and handles:
     * 1. Incrementing click counts
     * 2. Cache invalidation for analytics
     * 3. Error handling and retries
     * 
     * @param event The link click event to process
     */
    @RabbitListener(queues = RabbitMQConfig.LINK_CLICK_QUEUE)
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "analytics", key = "#event.username + '_overview'"),
        @CacheEvict(value = "analytics", key = "#event.username + '_detailed'"),
        @CacheEvict(value = "analytics", key = "#event.username + '_top_links'"),
        @CacheEvict(value = "linkPreviews", key = "#event.linkId"),
        // Invalidate analytics breakdown caches so new clicks show up immediately
        @CacheEvict(value = "analytics-referrers-v1", allEntries = true),
        @CacheEvict(value = "analytics-devices-v1", allEntries = true),
        @CacheEvict(value = "analytics-countries-v1", allEntries = true),
        @CacheEvict(value = "analytics-variants-v1", allEntries = true),
        @CacheEvict(value = "analytics-variants-by-link-v1", allEntries = true)
    })
    public void processLinkClick(LinkClickEvent event) {
        try {
            log.info("Processing click event for link {} by user {}", 
                    event.getLinkId(), event.getUsername());
            
            // Validate event
            if (event.getLinkId() == null || event.getClickedAt() == null) {
                log.error("Invalid click event received: {}", event);
                return;
            }
            
            // Find and update the link
            Link link = linkRepository.findById(event.getLinkId())
                    .orElseThrow(() -> new RuntimeException("Link not found: " + event.getLinkId()));
            
            // Increment click count
            link.setClickCount(link.getClickCount() + 1);
            link.setUpdatedAt(java.time.LocalDateTime.now());
            
            linkRepository.save(link);
            
            // Aggregate daily clicks (UTC day)
            java.time.LocalDate day = event.getClickedAt() != null ?
                    event.getClickedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate() :
                    java.time.LocalDate.now(java.time.ZoneOffset.UTC);
            aggregateRepository.upsertIncrement(event.getUsername(), event.getLinkId(), day);
            if (event.getVariantId() != null) {
                variantAggregateRepository.upsertIncrement(event.getUsername(), event.getLinkId(), event.getVariantId(), day);
            }

            // Variant-level aggregation is handled above when a variantId is present.

            // Unique visitor dedup via Redis (per user+link+day+visitor key)
            String visitorId = deriveVisitorId(event);
            if (visitorId != null && !visitorId.isBlank()) {
                String redisKey = String.format("uv:%s:%d:%s", event.getUsername(), event.getLinkId(), day);
                Long added = redisTemplate.opsForSet().add(redisKey, visitorId);
                // expire in 40 days to cover late events
                redisTemplate.expire(redisKey, java.time.Duration.ofDays(40));
                if (added != null && added > 0) {
                    aggregateRepository.incrementUnique(event.getUsername(), event.getLinkId(), day);
                    if (event.getVariantId() != null) {
                        variantAggregateRepository.incrementUnique(event.getUsername(), event.getLinkId(), event.getVariantId(), day);
                    }
                }
            }

            // Referrer aggregation (domain-level)
            String domain = extractDomain(event.getReferrer());
            if (domain != null) {
                referrerAggregateRepository.upsertIncrement(event.getUsername(), event.getLinkId(), day, domain);
                if (visitorId != null) {
                    String rKey = String.format("uvr:%s:%d:%s:%s", event.getUsername(), event.getLinkId(), day, domain);
                    Long addedR = redisTemplate.opsForSet().add(rKey, visitorId);
                    redisTemplate.expire(rKey, java.time.Duration.ofDays(40));
                    if (addedR != null && addedR > 0) {
                        referrerAggregateRepository.incrementUnique(event.getUsername(), event.getLinkId(), day, domain);
                    }
                }
            }

            // Optional: treat QR scans specially (when source=qr or utm_medium=qr)
            // Source aggregation (e.g., qr, email, social)
            String source = normalizeSource(event);
            if (source != null) {
                sourceAggregateRepository.upsertIncrement(event.getUsername(), event.getLinkId(), day, source);
                if (visitorId != null) {
                    String sKey = String.format("uvs:%s:%d:%s:%s", event.getUsername(), event.getLinkId(), day, source);
                    Long addedS = redisTemplate.opsForSet().add(sKey, visitorId);
                    redisTemplate.expire(sKey, java.time.Duration.ofDays(40));
                    if (addedS != null && addedS > 0) {
                        sourceAggregateRepository.incrementUnique(event.getUsername(), event.getLinkId(), day, source);
                    }
                }
            }

            // Device aggregation (simple UA classifier)
            String device = classifyDevice(event.getUserAgent());
            deviceAggregateRepository.upsertIncrement(event.getUsername(), event.getLinkId(), day, device);
            if (visitorId != null) {
                String dKey = String.format("uvd:%s:%d:%s:%s", event.getUsername(), event.getLinkId(), day, device);
                Long addedD = redisTemplate.opsForSet().add(dKey, visitorId);
                redisTemplate.expire(dKey, java.time.Duration.ofDays(40));
                if (addedD != null && addedD > 0) {
                    deviceAggregateRepository.incrementUnique(event.getUsername(), event.getLinkId(), day, device);
                }
            }

            // Geo aggregation (country from IP)
            String country = geoIpService.resolveCountryIso2(event.getClientIp());
            if (country != null) {
                geoAggregateRepository.upsertIncrement(event.getUsername(), event.getLinkId(), day, country);
                if (visitorId != null) {
                    String gKey = String.format("uvg:%s:%d:%s:%s", event.getUsername(), event.getLinkId(), day, country);
                    Long addedG = redisTemplate.opsForSet().add(gKey, visitorId);
                    redisTemplate.expire(gKey, java.time.Duration.ofDays(40));
                    if (addedG != null && addedG > 0) {
                        geoAggregateRepository.incrementUnique(event.getUsername(), event.getLinkId(), day, country);
                    }
                }
            }
            
            // Log analytics event for potential future processing
            logAnalyticsEvent(event);
            
            log.debug("Successfully processed click event for link {}, new count: {}", 
                    event.getLinkId(), link.getClickCount());

            // Emit webhook (best-effort)
            webhookService.emitLinkClick(event.getUsername(), event.getLinkId(), event.getTargetUrl(), event.getReferrer(), event.getClientIp(), event.getUserAgent());
            
        } catch (Exception e) {
            log.error("Failed to process click event for link {}: {}", 
                    event.getLinkId(), e.getMessage(), e);
            throw e; // Rethrow to trigger RabbitMQ retry mechanism
        }
    }
    
    /**
     * Log detailed analytics event for future processing.
     * This could be extended to store in a time-series database,
     * send to analytics services, or aggregate daily statistics.
     * 
     * @param event The click event to log
     */
    private void logAnalyticsEvent(LinkClickEvent event) {
        // For now, just log the event details
        // In a production system, this could:
        // 1. Store in InfluxDB/TimescaleDB for time-series analytics
        // 2. Send to Google Analytics or Mixpanel
        // 3. Aggregate hourly/daily statistics
        // 4. Detect fraud or bot traffic
        
        log.info("Analytics Event - Link: {}, User: {}, IP: {}, Time: {}, UserAgent: {}", 
                event.getLinkId(),
                event.getUsername(),
                maskIpAddress(event.getClientIp()),
                event.getClickedAt(),
                event.getUserAgent() != null ? event.getUserAgent().substring(0, 
                    Math.min(50, event.getUserAgent().length())) : "unknown"
        );
    }
    
    /**
     * Mask IP address for privacy compliance (GDPR, etc.)
     * 
     * @param ipAddress Original IP address
     * @return Masked IP address
     */
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null) return "unknown";
        
        // Mask last octet for IPv4: 192.168.1.123 -> 192.168.1.xxx
        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            return String.format("%s.%s.%s.xxx", parts[0], parts[1], parts[2]);
        }
        
        // For IPv6 or other formats, just return "masked"
        return "masked";
    }

    private String deriveVisitorId(LinkClickEvent event) {
        // Prefer sessionId, fall back to IP + minimal UA hash
        if (event.getSessionId() != null && !event.getSessionId().isBlank()) {
            return "s:" + event.getSessionId();
        }
        String ip = event.getClientIp();
        String ua = event.getUserAgent();
        if (ip == null && ua == null) return null;
        String uaSig = (ua == null) ? "" : Integer.toHexString(ua.hashCode());
        return "i:" + (ip != null ? ip : "?") + ":u:" + uaSig;
    }

    private String extractDomain(String referrer) {
        if (referrer == null || referrer.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(referrer);
            String host = uri.getHost();
            if (host == null) return null;
            return host.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private String classifyDevice(String ua) {
        if (ua == null) return "other";
        String s = ua.toLowerCase();
        if (s.contains("tablet") || s.contains("ipad")) return "tablet";
        if (s.contains("mobi") || s.contains("iphone") || s.contains("android")) return "mobile";
        if (s.contains("windows") || s.contains("macintosh") || s.contains("linux")) return "desktop";
        return "other";
    }

    private String normalizeSource(LinkClickEvent event) {
        try {
            if (event == null) return null;
            if (event.getSource() != null && !event.getSource().isBlank()) {
                return event.getSource().toLowerCase();
            }
            String utmMedium = event.getUtmMedium();
            if (utmMedium != null && !utmMedium.isBlank()) {
                return utmMedium.toLowerCase();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
