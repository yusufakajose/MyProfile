package com.linkgrove.api.worker;

import com.linkgrove.api.model.WebhookDelivery;
import com.linkgrove.api.repository.WebhookDeliveryRepository;
import com.linkgrove.api.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryWorker {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookService webhookService;

    // Every 30s poll for due retries
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    @Transactional
    public void processDueRetries() {
        List<WebhookDelivery> due = deliveryRepository.findDueRetries(LocalDateTime.now());
        if (due.isEmpty()) return;
        for (WebhookDelivery d : due) {
            try {
                webhookService.resend(d.getId());
            } catch (Exception e) {
                log.warn("Retry failed for delivery {}: {}", d.getId(), e.toString());
            }
        }
    }
}


