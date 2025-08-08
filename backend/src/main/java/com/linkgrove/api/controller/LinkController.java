package com.linkgrove.api.controller;

import com.linkgrove.api.dto.*;
import com.linkgrove.api.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
                                          @RequestParam(required = false) String q) {
        String username = authentication.getName();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
        if (q != null && !q.isBlank()) {
            Page<LinkResponse> result = linkService.searchUserLinks(username, q.trim(), pageable);
            return ResponseEntity.ok(result);
        }
        Page<LinkResponse> result = linkService.getUserLinksPage(username, pageable);
        return ResponseEntity.ok(result);
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
}
