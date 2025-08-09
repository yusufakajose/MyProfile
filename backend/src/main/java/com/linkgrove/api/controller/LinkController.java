package com.linkgrove.api.controller;

import com.linkgrove.api.dto.*;
import com.linkgrove.api.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.linkgrove.api.dto.LinkVariantRequest;
import com.linkgrove.api.dto.LinkVariantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LinkController {

    private final LinkService linkService;
    private final com.linkgrove.api.service.LinkVariantService linkVariantService;

    @PostMapping
    public ResponseEntity<LinkResponse> createLink(
            Authentication authentication,
            @Valid @RequestBody CreateLinkRequest request) {
        String username = authentication.getName();
        LinkResponse response = linkService.createLink(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getUserLinks(Authentication authentication,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "12") int size,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) List<String> tags,
                                          @RequestParam(required = false, defaultValue = "order") String sort,
                                          @RequestParam(required = false, defaultValue = "all") String status) {
        String username = authentication.getName();
        org.springframework.data.domain.Sort springSort = mapSort(sort);
        Boolean active = mapStatus(status);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)), springSort);
        if (q != null && !q.isBlank()) {
            Page<LinkResponse> result = linkService.searchUserLinks(username, q.trim(), tags, active, pageable);
            return ResponseEntity.ok(result);
        }
        Page<LinkResponse> result = linkService.getUserLinksPage(username, tags, active, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> listUserTags(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(linkService.listUserTagNames(username));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportLinksCsv(Authentication authentication,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) List<String> tags,
                                                 @RequestParam(required = false, defaultValue = "order") String sort,
                                                 @RequestParam(required = false, defaultValue = "all") String status) {
        String username = authentication.getName();
        org.springframework.data.domain.Sort springSort = mapSort(sort);
        Boolean active = mapStatus(status);
        Pageable pageable = PageRequest.of(0, 1000, springSort);
        Page<LinkResponse> page;
        if (q != null && !q.isBlank()) {
            page = linkService.searchUserLinks(username, q.trim(), tags, active, pageable);
        } else {
            page = linkService.getUserLinksPage(username, tags, active, pageable);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("id,title,url,description,alias,tags,isActive,displayOrder,clickCount,createdAt,updatedAt\n");
        for (LinkResponse l : page.getContent()) {
            String tagStr = l.getTags() != null ? String.join("|", l.getTags()) : "";
            sb.append(String.format("%d,%s,%s,%s,%s,%s,%s,%d,%d,%s,%s\n",
                    l.getId(),
                    escapeCsv(l.getTitle()),
                    escapeCsv(l.getUrl()),
                    escapeCsv(l.getDescription()),
                    escapeCsv(l.getAlias()),
                    escapeCsv(tagStr),
                    String.valueOf(l.getIsActive()),
                    l.getDisplayOrder(),
                    l.getClickCount(),
                    l.getCreatedAt() != null ? l.getCreatedAt().toString() : "",
                    l.getUpdatedAt() != null ? l.getUpdatedAt().toString() : ""
            ));
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=links.csv")
                .body(sb.toString());
    }

    private org.springframework.data.domain.Sort mapSort(String sort) {
        if (sort == null) return org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("displayOrder"));
        return switch (sort) {
            case "created_desc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"));
            case "created_asc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("createdAt"));
            case "updated_desc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("updatedAt"));
            case "updated_asc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("updatedAt"));
            case "clicks_desc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("clickCount"));
            case "clicks_asc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("clickCount"));
            default -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("displayOrder"));
        };
    }

    private Boolean mapStatus(String status) {
        if (status == null) return null;
        String s = status.toLowerCase();
        if (s.equals("active")) return Boolean.TRUE;
        if (s.equals("inactive")) return Boolean.FALSE;
        return null;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        String v = val.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    @GetMapping("/{linkId}")
    public ResponseEntity<LinkResponse> getLinkById(
            Authentication authentication,
            @PathVariable Long linkId) {
        String username = authentication.getName();
        LinkResponse link = linkService.getLinkById(username, linkId);
        return ResponseEntity.ok(link);
    }

    @PutMapping("/{linkId}")
    public ResponseEntity<LinkResponse> updateLink(
            Authentication authentication,
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateLinkRequest request) {
        String username = authentication.getName();
        LinkResponse response = linkService.updateLink(username, linkId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> deleteLink(
            Authentication authentication,
            @PathVariable Long linkId) {
        String username = authentication.getName();
        linkService.deleteLink(username, linkId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderLinks(
            Authentication authentication,
            @RequestBody List<Long> linkIds) {
        String username = authentication.getName();
        linkService.reorderLinks(username, linkIds);
        return ResponseEntity.ok().build();
    }

    // Variants CRUD
    @GetMapping("/{linkId}/variants")
    public ResponseEntity<List<LinkVariantResponse>> listVariants(
            Authentication authentication,
            @PathVariable Long linkId) {
        String username = authentication.getName();
        return ResponseEntity.ok(linkVariantService.listVariants(username, linkId));
    }

    @PostMapping("/{linkId}/variants")
    public ResponseEntity<LinkVariantResponse> addVariant(
            Authentication authentication,
            @PathVariable Long linkId,
            @Valid @RequestBody LinkVariantRequest request) {
        String username = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(linkVariantService.addVariant(username, linkId, request));
    }

    @PutMapping("/{linkId}/variants/{variantId}")
    public ResponseEntity<LinkVariantResponse> updateVariant(
            Authentication authentication,
            @PathVariable Long linkId,
            @PathVariable Long variantId,
            @Valid @RequestBody LinkVariantRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(linkVariantService.updateVariant(username, linkId, variantId, request));
    }

    @DeleteMapping("/{linkId}/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(
            Authentication authentication,
            @PathVariable Long linkId,
            @PathVariable Long variantId) {
        String username = authentication.getName();
        linkVariantService.deleteVariant(username, linkId, variantId);
        return ResponseEntity.noContent().build();
    }
}
