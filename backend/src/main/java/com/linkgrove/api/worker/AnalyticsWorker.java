package com.linkgrove.api.worker;

import com.linkgrove.api.config.RabbitMQConfig;
import com.linkgrove.api.event.LinkClickEvent;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.LinkClickDailyAggregateRepository;
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
        @CacheEvict(value = "linkPreviews", key = "#event.linkId")
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
            
            // Log analytics event for potential future processing
            logAnalyticsEvent(event);
            
            log.debug("Successfully processed click event for link {}, new count: {}", 
                    event.getLinkId(), link.getClickCount());
            
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
}
