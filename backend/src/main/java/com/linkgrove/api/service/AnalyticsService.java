package com.linkgrove.api.service;

import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.UserRepository;
import com.linkgrove.api.repository.LinkClickDailyAggregateRepository;
import com.linkgrove.api.repository.LinkReferrerDailyAggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final LinkClickDailyAggregateRepository aggregateRepository;
    private final LinkReferrerDailyAggregateRepository referrerAggregateRepository;

    @Cacheable(value = "analytics", key = "#username + '_overview'")
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAnalyticsOverview(String username) {
        log.info("Fetching analytics overview from database for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Link> userLinks = linkRepository.findByUserOrderByDisplayOrderAsc(user);
        
        long totalClicks = userLinks.stream()
                .mapToLong(Link::getClickCount)
                .sum();
        
        long totalLinks = userLinks.size();
        long activeLinks = userLinks.stream()
                .mapToLong(link -> link.getIsActive() ? 1 : 0)
                .sum();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("username", username);
        analytics.put("totalClicks", totalClicks);
        analytics.put("totalLinks", totalLinks);
        analytics.put("activeLinks", activeLinks);
        analytics.put("averageClicksPerLink", totalLinks > 0 ? (double) totalClicks / totalLinks : 0.0);
        
        return analytics;
    }

    @Cacheable(value = "analytics", key = "#username + '_detailed'")
    @Transactional(readOnly = true)
    public Map<String, Object> getUserDetailedAnalytics(String username) {
        log.info("Fetching detailed analytics from database for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Link> userLinks = linkRepository.findByUserOrderByDisplayOrderAsc(user);
        
        Map<String, Object> detailedAnalytics = new HashMap<>();
        detailedAnalytics.put("username", username);
        detailedAnalytics.put("linkAnalytics", userLinks.stream()
                .map(link -> {
                    Map<String, Object> linkData = new HashMap<>();
                    linkData.put("id", link.getId());
                    linkData.put("title", link.getTitle());
                    linkData.put("url", link.getUrl());
                    linkData.put("clickCount", link.getClickCount());
                    linkData.put("isActive", link.getIsActive());
                    linkData.put("displayOrder", link.getDisplayOrder());
                    linkData.put("createdAt", link.getCreatedAt());
                    linkData.put("updatedAt", link.getUpdatedAt());
                    return linkData;
                })
                .toList());
        
        return detailedAnalytics;
    }

    @Cacheable(value = "analytics", key = "#username + '_top_links'")
    @Transactional(readOnly = true)
    public Map<String, Object> getTopPerformingLinks(String username) {
        log.info("Fetching top performing links from database for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Link> userLinks = linkRepository.findByUserOrderByDisplayOrderAsc(user);
        
        List<Map<String, Object>> topLinks = userLinks.stream()
                .filter(link -> link.getClickCount() > 0)
                .sorted((a, b) -> Long.compare(b.getClickCount(), a.getClickCount()))
                .limit(5)
                .map(link -> {
                    Map<String, Object> linkData = new HashMap<>();
                    linkData.put("id", link.getId());
                    linkData.put("title", link.getTitle());
                    linkData.put("url", link.getUrl());
                    linkData.put("clickCount", link.getClickCount());
                    linkData.put("displayOrder", link.getDisplayOrder());
                    return linkData;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("topLinks", topLinks);
        
        return result;
    }

    @Cacheable(value = "analytics", key = "#username + '_dashboard_summary'")
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(String username) {
        log.info("Fetching dashboard summary from database for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Link> userLinks = linkRepository.findByUserOrderByDisplayOrderAsc(user);
        
        long totalClicks = userLinks.stream()
                .mapToLong(Link::getClickCount)
                .sum();
        
        long totalLinks = userLinks.size();
        long activeLinks = userLinks.stream()
                .mapToLong(link -> link.getIsActive() ? 1 : 0)
                .sum();

        // Calculate engagement metrics
        double avgClicksPerLink = totalLinks > 0 ? (double) totalClicks / totalLinks : 0.0;
        double engagementRate = totalLinks > 0 ? (double) activeLinks / totalLinks * 100 : 0.0;
        
        // Find most popular link
        Link mostPopularLink = userLinks.stream()
                .max((a, b) -> Long.compare(a.getClickCount(), b.getClickCount()))
                .orElse(null);

        Map<String, Object> summary = new HashMap<>();
        summary.put("username", username);
        summary.put("totalClicks", totalClicks);
        summary.put("totalLinks", totalLinks);
        summary.put("activeLinks", activeLinks);
        summary.put("inactiveLinks", totalLinks - activeLinks);
        summary.put("averageClicksPerLink", Math.round(avgClicksPerLink * 100.0) / 100.0);
        summary.put("engagementRate", Math.round(engagementRate * 100.0) / 100.0);
        summary.put("mostPopularLink", mostPopularLink != null ? Map.of(
            "id", mostPopularLink.getId(),
            "title", mostPopularLink.getTitle(),
            "clickCount", mostPopularLink.getClickCount()
        ) : null);
        
        return summary;
    }

    @Cacheable(value = "analytics", key = "#username + '_timeseries_' + #days")
    @Transactional(readOnly = true)
    public Map<String, Object> getTimeseriesData(String username, int days) {
        log.info("Fetching timeseries data for user: {} for last {} days", username, days);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.time.LocalDate end = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        java.time.LocalDate start = end.minusDays(Math.max(0, days - 1));

        var aggregates = aggregateRepository.findRange(user.getUsername(), start, end);
        var byDay = new java.util.HashMap<java.time.LocalDate, Long>();
        var byDayUnique = new java.util.HashMap<java.time.LocalDate, Long>();
        for (var a : aggregates) {
            byDay.merge(a.getDay(), a.getClicks(), Long::sum);
            byDayUnique.merge(a.getDay(), a.getUniqueVisitors(), Long::sum);
        }

        java.util.List<java.util.Map<String, Object>> timeseriesData = new java.util.ArrayList<>();
        long totalClicks = 0;
        for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long clicks = byDay.getOrDefault(d, 0L);
            totalClicks += clicks;
            java.util.Map<String, Object> dayData = new java.util.HashMap<>();
            dayData.put("date", d.toString());
            dayData.put("clicks", clicks);
            long uniques = byDayUnique.getOrDefault(d, 0L);
            dayData.put("uniqueVisitors", uniques);
            timeseriesData.add(dayData);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("username", username);
        result.put("period", days + " days");
        result.put("timeseriesData", timeseriesData);
        result.put("totalClicks", totalClicks);
        result.put("averageDailyClicks", timeseriesData.isEmpty() ? 0.0 :
                Math.round((double) totalClicks / timeseriesData.size() * 100.0) / 100.0);

        return result;
    }

    @Cacheable(value = "analytics", key = "#username + '_link_' + #linkId + '_timeseries_' + #days")
    @Transactional(readOnly = true)
    public Map<String, Object> getLinkTimeseriesData(String username, Long linkId, int days) {
        log.info("Fetching per-link timeseries for user: {}, linkId: {} for last {} days", username, linkId, days);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify link belongs to user
        Link link = linkRepository.findByIdAndUser(linkId, user)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        java.time.LocalDate end = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        java.time.LocalDate start = end.minusDays(Math.max(0, days - 1));

        var aggregates = aggregateRepository.findRangeForLink(user.getUsername(), link.getId(), start, end);
        var byDay = new java.util.HashMap<java.time.LocalDate, Long>();
        var byDayUnique = new java.util.HashMap<java.time.LocalDate, Long>();
        for (var a : aggregates) {
            byDay.merge(a.getDay(), a.getClicks(), Long::sum);
            byDayUnique.merge(a.getDay(), a.getUniqueVisitors(), Long::sum);
        }

        java.util.List<java.util.Map<String, Object>> timeseriesData = new java.util.ArrayList<>();
        long totalClicks = 0;
        for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long clicks = byDay.getOrDefault(d, 0L);
            totalClicks += clicks;
            java.util.Map<String, Object> dayData = new java.util.HashMap<>();
            dayData.put("date", d.toString());
            dayData.put("clicks", clicks);
            long uniques = byDayUnique.getOrDefault(d, 0L);
            dayData.put("uniqueVisitors", uniques);
            timeseriesData.add(dayData);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("username", username);
        result.put("linkId", link.getId());
        result.put("title", link.getTitle());
        result.put("period", days + " days");
        result.put("timeseriesData", timeseriesData);
        result.put("totalClicks", totalClicks);
        result.put("averageDailyClicks", timeseriesData.isEmpty() ? 0.0 :
                Math.round((double) totalClicks / timeseriesData.size() * 100.0) / 100.0);

        return result;
    }

    @Cacheable(value = "analytics", key = "#username + '_referrers_' + #days")
    @Transactional(readOnly = true)
    public Map<String, Object> getReferrerBreakdown(String username, int days) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.time.LocalDate end = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        java.time.LocalDate start = end.minusDays(Math.max(0, days - 1));

        var rows = referrerAggregateRepository.findRange(user.getUsername(), start, end);
        Map<String, Map<String, Long>> byRef = new java.util.HashMap<>();
        for (var r : rows) {
            var m = byRef.computeIfAbsent(r.getReferrerDomain(), k -> new java.util.HashMap<>());
            m.merge("clicks", r.getClicks(), Long::sum);
            m.merge("uniqueVisitors", r.getUniqueVisitors(), Long::sum);
        }
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        for (var e : byRef.entrySet()) {
            var m = new java.util.HashMap<String, Object>();
            m.put("referrerDomain", e.getKey());
            m.put("clicks", e.getValue().getOrDefault("clicks", 0L));
            m.put("uniqueVisitors", e.getValue().getOrDefault("uniqueVisitors", 0L));
            list.add(m);
        }
        list.sort((a, b) -> Long.compare((Long)b.get("clicks"), (Long)a.get("clicks")));
        return java.util.Map.of(
                "username", username,
                "period", days + " days",
                "referrers", list
        );
    }
}
