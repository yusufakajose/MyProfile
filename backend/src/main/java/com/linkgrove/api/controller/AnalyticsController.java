package com.linkgrove.api.controller;

import com.linkgrove.api.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/dashboard/timeseries/by-link")
    public ResponseEntity<?> getTimeseriesDataByLink(Authentication authentication,
                                                     @RequestParam Long linkId,
                                                     @RequestParam(defaultValue = "7") int days) {
        String username = authentication.getName();
        Map<String, Object> timeseries = analyticsService.getLinkTimeseriesData(username, linkId, days);
        return ResponseEntity.ok(timeseries);
    }

    @GetMapping(value = "/export/timeseries", produces = "text/csv")
    public ResponseEntity<String> exportTimeseriesCsv(Authentication authentication,
                                                      @RequestParam(defaultValue = "7") int days) {
        String username = authentication.getName();
        Map<String, Object> data = analyticsService.getTimeseriesData(username, days);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("timeseriesData");
        StringBuilder sb = new StringBuilder();
        sb.append("date,clicks,uniqueVisitors\n");
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String date = String.valueOf(row.getOrDefault("date", ""));
                String clicks = String.valueOf(row.getOrDefault("clicks", 0));
                String uniques = String.valueOf(row.getOrDefault("uniqueVisitors", 0));
                sb.append(escapeCsv(date)).append(',')
                  .append(clicks).append(',')
                  .append(uniques).append('\n');
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_timeseries_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    @GetMapping(value = "/export/top-links", produces = "text/csv")
    public ResponseEntity<String> exportTopLinksCsv(Authentication authentication,
                                                    @RequestParam(defaultValue = "5") int limit) {
        String username = authentication.getName();
        Map<String, Object> top = analyticsService.getTopPerformingLinks(username);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> links = (List<Map<String, Object>>) top.get("topLinks");

        String header = "id,title,url,clickCount,displayOrder\n";
        String body = (links == null ? List.<Map<String,Object>>of() : links).stream()
                .limit(Math.max(0, limit))
                .map(link -> String.join(",",
                        String.valueOf(link.getOrDefault("id", "")),
                        escapeCsv(String.valueOf(link.getOrDefault("title", ""))),
                        escapeCsv(String.valueOf(link.getOrDefault("url", ""))),
                        String.valueOf(link.getOrDefault("clickCount", 0)),
                        String.valueOf(link.getOrDefault("displayOrder", ""))
                ))
                .collect(Collectors.joining("\n"));

        String csv = header + body + (body.isEmpty() ? "" : "\n");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_top_links_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(",") || value.contains("\n") || value.contains("\"");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? '"' + escaped + '"' : escaped;
    }
}
