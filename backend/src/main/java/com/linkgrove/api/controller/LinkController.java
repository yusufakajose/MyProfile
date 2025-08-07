package com.linkgrove.api.controller;

import com.linkgrove.api.dto.*;
import com.linkgrove.api.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<List<LinkResponse>> getUserLinks(Authentication authentication) {
        String username = authentication.getName();
        List<LinkResponse> links = linkService.getUserLinks(username);
        return ResponseEntity.ok(links);
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
