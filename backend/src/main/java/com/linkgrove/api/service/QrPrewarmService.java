package com.linkgrove.api.service;

import com.linkgrove.api.model.Link;
import com.linkgrove.api.repository.LinkClickDailyAggregateRepository;
import com.linkgrove.api.repository.LinkRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
// removed unused imports
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrPrewarmService {

    private final QrCodeService qrCodeService;
    private final LinkRepository linkRepository;
    private final LinkClickDailyAggregateRepository clickAggRepo;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${qr.prewarm.enabled:true}")
    private boolean enabled;

    @Value("${qr.prewarm.topN:50}")
    private int topN;

    @Value("${qr.prewarm.lockTtlSeconds:300}")
    private long lockTtlSeconds;

    // Warm some common presets
    private static final int[] SIZES = {256, 512};
    private static final String[] ECCS = {"M", "H"};

    public void onLinkCreatedOrUpdated(Link link) {
        if (!enabled || link == null || Boolean.FALSE.equals(link.getIsActive())) return;
        String lockKey = "qr:prewarm:link:" + link.getId();
        if (!acquireLock(lockKey, lockTtlSeconds)) return;
        try {
            prewarmForLink(link.getId());
        } finally {
            releaseLock(lockKey);
        }
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void scheduledDailyPrewarm() {
        if (!enabled) return;
        String lockKey = "qr:prewarm:daily:" + LocalDate.now();
        if (!acquireLock(lockKey, 3600)) return;
        try {
            List<Long> popular = findPopularLinkIds(topN);
            log.info("Prewarming QR codes for top {} links ({} found)", topN, popular.size());
            for (Long id : popular) {
                prewarmForLink(id);
            }
        } finally {
            releaseLock(lockKey);
        }
    }

    private void prewarmForLink(Long linkId) {
        Timer.Sample sample = Timer.start();
        try {
            for (int size : SIZES) {
                for (String ecc : ECCS) {
                    qrCodeService.generatePng("/r/" + linkId, size, 1, null, null, null,
                            com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.valueOf(ecc));
                    qrCodeService.generateSvg("/r/" + linkId, size, 1, null, null,
                            com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.valueOf(ecc));
                }
            }
            log.debug("Prewarmed QR cache for link {}", linkId);
        } catch (Exception e) {
            log.warn("QR prewarm failed for link {}: {}", linkId, e.getMessage());
        } finally {
            sample.stop(meterRegistry.timer("qr.prewarm.duration"));
        }
    }

    private List<Long> findPopularLinkIds(int limit) {
        // Simple heuristic: sum clicks for last 7 days and pick topN
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);
        var allUsers = linkRepository.findAll().stream().map(l -> l.getUser().getUsername()).collect(Collectors.toSet());
        // Aggregate per-link clicks across all users
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        for (String u : allUsers) {
            var rows = clickAggRepo.findRange(u, start, end);
            for (var row : rows) {
                counts.merge(row.getLink().getId(), row.getClicks(), Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(java.util.Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean acquireLock(String key, long ttlSeconds) {
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            return false;
        }
    }

    private void releaseLock(String key) {
        try { redisTemplate.delete(key); } catch (Exception ignored) {}
    }
}


