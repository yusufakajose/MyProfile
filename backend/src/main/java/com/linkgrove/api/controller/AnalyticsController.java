package com.linkgrove.api.controller;

import com.linkgrove.api.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
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
                                               @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        Map<String, Object> timeseries = analyticsService.getTimeseriesData(username, days);
        return ResponseEntity.ok(timeseries);
    }

    @GetMapping("/dashboard/timeseries/by-link")
    public ResponseEntity<?> getTimeseriesDataByLink(Authentication authentication,
                                                     @RequestParam @Min(1) Long linkId,
                                                     @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        Map<String, Object> timeseries = analyticsService.getLinkTimeseriesData(username, linkId, days);
        return ResponseEntity.ok(timeseries);
    }

    @GetMapping("/referrers")
    public ResponseEntity<?> getReferrerBreakdown(Authentication authentication,
                                                  @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        return ResponseEntity.ok(analyticsService.getReferrerBreakdown(username, days));
    }

    @GetMapping("/devices")
    public ResponseEntity<?> getDeviceBreakdown(Authentication authentication,
                                                @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        return ResponseEntity.ok(analyticsService.getDeviceBreakdown(username, days));
    }

    @GetMapping("/countries")
    public ResponseEntity<?> getCountryBreakdown(Authentication authentication,
                                                 @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        return ResponseEntity.ok(analyticsService.getCountryBreakdown(username, days));
    }

    @GetMapping("/sources")
    public ResponseEntity<?> getSourceBreakdown(Authentication authentication,
                                                @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        return ResponseEntity.ok(analyticsService.getSourceBreakdown(username, days));
    }

    @GetMapping("/sources/by-link")
    public ResponseEntity<?> getSourceBreakdownByLink(Authentication authentication,
                                                      @RequestParam @Min(1) Long linkId,
                                                      @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        return ResponseEntity.ok(analyticsService.getSourceBreakdownByLink(username, linkId, days));
    }

    @GetMapping(value = "/export/countries", produces = "text/csv")
    public ResponseEntity<String> exportCountriesCsv(Authentication authentication,
                                                     @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        var data = analyticsService.getCountryBreakdown(username, days);
        var rows = data.getCountries();
        StringBuilder sb = new StringBuilder();
        sb.append("country,clicks,uniqueVisitors\n");
        if (rows != null) {
            for (var row : rows) {
                String c = String.valueOf(row.getCountry());
                String clicks = String.valueOf(row.getClicks());
                String uniques = String.valueOf(row.getUniqueVisitors());
                sb.append(escapeCsv(c)).append(',').append(clicks).append(',').append(uniques).append('\n');
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_countries_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    @GetMapping(value = "/export/sources", produces = "text/csv")
    public ResponseEntity<String> exportSourcesCsv(Authentication authentication,
                                                   @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        var data = analyticsService.getSourceBreakdown(username, days);
        var rows = data.getSources();
        StringBuilder sb = new StringBuilder();
        sb.append("source,clicks,uniqueVisitors\n");
        if (rows != null) {
            for (var row : rows) {
                String s = String.valueOf(row.getSource());
                String clicks = String.valueOf(row.getClicks());
                String uniques = String.valueOf(row.getUniqueVisitors());
                sb.append(escapeCsv(s)).append(',').append(clicks).append(',').append(uniques).append('\n');
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_sources_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    @GetMapping(value = "/export/sources/by-link", produces = "text/csv")
    public ResponseEntity<String> exportSourcesByLinkCsv(Authentication authentication,
                                                         @RequestParam @Min(1) Long linkId,
                                                         @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        var data = analyticsService.getSourceBreakdownByLink(username, linkId, days);
        var rows = data.getSources();
        StringBuilder sb = new StringBuilder();
        sb.append("source,clicks,uniqueVisitors\n");
        if (rows != null) {
            for (var row : rows) {
                String s = String.valueOf(row.getSource());
                String clicks = String.valueOf(row.getClicks());
                String uniques = String.valueOf(row.getUniqueVisitors());
                sb.append(escapeCsv(s)).append(',').append(clicks).append(',').append(uniques).append('\n');
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_sources_link_" + linkId + "_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    @GetMapping("/variants")
    public ResponseEntity<?> getVariantBreakdown(Authentication authentication,
                                                 @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        return ResponseEntity.ok(analyticsService.getVariantBreakdown(username, days));
    }

    @GetMapping("/variants/by-link")
    public ResponseEntity<?> getVariantBreakdownByLink(Authentication authentication,
                                                       @RequestParam @Min(1) Long linkId,
                                                       @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        return ResponseEntity.ok(analyticsService.getVariantBreakdownByLink(username, linkId, days));
    }

    @GetMapping(value = "/export/referrers", produces = "text/csv")
    public ResponseEntity<String> exportReferrersCsv(Authentication authentication,
                                                     @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        var data = analyticsService.getReferrerBreakdown(username, days);
        var rows = data.getReferrers();
        StringBuilder sb = new StringBuilder();
        sb.append("referrerDomain,clicks,uniqueVisitors\n");
        if (rows != null) {
            for (var row : rows) {
                String ref = String.valueOf(row.getReferrerDomain());
                String clicks = String.valueOf(row.getClicks());
                String uniques = String.valueOf(row.getUniqueVisitors());
                sb.append(escapeCsv(ref)).append(',').append(clicks).append(',').append(uniques).append('\n');
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_referrers_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    @GetMapping(value = "/export/devices", produces = "text/csv")
    public ResponseEntity<String> exportDevicesCsv(Authentication authentication,
                                                   @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        var data = analyticsService.getDeviceBreakdown(username, days);
        var rows = data.getDevices();
        StringBuilder sb = new StringBuilder();
        sb.append("deviceType,clicks,uniqueVisitors\n");
        if (rows != null) {
            for (var row : rows) {
                String dev = String.valueOf(row.getDeviceType());
                String clicks = String.valueOf(row.getClicks());
                String uniques = String.valueOf(row.getUniqueVisitors());
                sb.append(escapeCsv(dev)).append(',').append(clicks).append(',').append(uniques).append('\n');
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_devices_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    @GetMapping(value = "/export/timeseries", produces = "text/csv")
    public ResponseEntity<String> exportTimeseriesCsv(Authentication authentication,
                                                      @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
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
                                                    @RequestParam(defaultValue = "5") @Min(1) @Max(1000) int limit) {
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

    @GetMapping(value = "/export/timeseries/by-link", produces = "text/csv")
    public ResponseEntity<String> exportTimeseriesByLinkCsv(Authentication authentication,
                                                            @RequestParam @Min(1) Long linkId,
                                                            @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        Map<String, Object> data = analyticsService.getLinkTimeseriesData(username, linkId, days);
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
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_link_" + linkId + "_timeseries_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    @GetMapping(value = "/export/variants/by-link", produces = "text/csv")
    public ResponseEntity<String> exportVariantsByLinkCsv(Authentication authentication,
                                                          @RequestParam @Min(1) Long linkId,
                                                          @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {
        String username = authentication.getName();
        Map<String, Object> data = analyticsService.getVariantBreakdownByLink(username, linkId, days);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("variants");
        StringBuilder sb = new StringBuilder();
        sb.append("variantId,variantTitle,clicks,uniqueVisitors\n");
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String id = String.valueOf(row.getOrDefault("variantId", ""));
                String title = String.valueOf(row.getOrDefault("variantTitle", ""));
                String clicks = String.valueOf(row.getOrDefault("clicks", 0));
                String uniques = String.valueOf(row.getOrDefault("uniqueVisitors", 0));
                sb.append(id).append(',')
                  .append(escapeCsv(title)).append(',')
                  .append(clicks).append(',')
                  .append(uniques).append('\n');
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_link_" + linkId + "_variants_" + days + "d_" + username + ".csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(",") || value.contains("\n") || value.contains("\"");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? '"' + escaped + '"' : escaped;
    }
}
