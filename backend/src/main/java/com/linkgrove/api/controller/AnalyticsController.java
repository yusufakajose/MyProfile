package com.linkgrove.api.controller;

import com.linkgrove.api.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<?> getAnalyticsOverview(Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> analytics = analyticsService.getUserAnalyticsOverview(username);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/detailed")
    public ResponseEntity<?> getDetailedAnalytics(Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> detailed = analyticsService.getUserDetailedAnalytics(username);
        return ResponseEntity.ok(detailed);
    }

    @GetMapping("/top-links")
    public ResponseEntity<?> getTopLinks(Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> top = analyticsService.getTopPerformingLinks(username);
        return ResponseEntity.ok(top);
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<?> getDashboardSummary(Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> summary = analyticsService.getDashboardSummary(username);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/dashboard/timeseries")
    public ResponseEntity<?> getTimeseriesData(Authentication authentication,
                                               @RequestParam(defaultValue = "7") int days) {
        String username = authentication.getName();
        Map<String, Object> timeseries = analyticsService.getTimeseriesData(username, days);
        return ResponseEntity.ok(timeseries);
    }
}
