package com.linkgrove.api.service;

import com.linkgrove.api.dto.*;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.UserRepository;
import com.linkgrove.api.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final TagRepository tagRepository;
    private final QrPrewarmService qrPrewarmService;

    @Caching(evict = {
        @CacheEvict(value = "publicProfiles", key = "#username"),
        @CacheEvict(value = "userLinks", key = "#username"),
        @CacheEvict(value = "analytics", key = "#username + '_overview'"),
        @CacheEvict(value = "analytics", key = "#username + '_detailed'"),
        @CacheEvict(value = "analytics", key = "#username + '_top_links'")
    })
    @Transactional
    public LinkResponse createLink(String username, CreateLinkRequest request) {
        log.info("Creating new link for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer maxOrder = linkRepository.findMaxDisplayOrderForUser(user);
        int nextOrder = (maxOrder != null) ? maxOrder + 1 : 1;

        Link link = Link.builder()
                .title(sanitizeTitle(request.getTitle()))
                .url(sanitizeUrl(request.getUrl()))
                .description(sanitizeDescription(request.getDescription()))
                .user(user)
                .displayOrder(nextOrder)
                .isActive(true)
                .clickCount(0L)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();

        if (request.getAlias() != null && !request.getAlias().isBlank()) {
            String alias = normalizeAlias(request.getAlias());
            linkRepository.findByAlias(alias).ifPresent(existing -> {
                throw new RuntimeException("Alias already in use");
            });
            link.setAlias(alias);
        }

        if (request.getTags() != null) {
            link.setTags(resolveTags(request.getTags()));
        }
        Link saved = linkRepository.save(link);
        // Fire-and-forget prewarm
        try { qrPrewarmService.onLinkCreatedOrUpdated(saved); } catch (Exception ignored) {}
        return mapToLinkResponse(saved);
    }

    @Cacheable(value = "userLinks", key = "#username")
    @Transactional(readOnly = true)
    public List<LinkResponse> getUserLinks(String username) {
        log.info("Fetching user links from database for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return linkRepository.findByUserOrderByDisplayOrderAsc(user)
                .stream()
                .map(this::mapToLinkResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LinkResponse> getUserLinksPage(String username, java.util.List<String> tagNames, Boolean active, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        java.util.List<String> norm = normalize(tagNames);
        Page<Link> page = norm == null || norm.isEmpty()
                ? linkRepository.findByUserNoTags(user, active, pageable)
                : linkRepository.findByUserWithTags(user, norm, active, pageable);
        return page.map(this::mapToLinkResponse);
    }

    @Transactional(readOnly = true)
    public Page<LinkResponse> searchUserLinks(String username, String query, java.util.List<String> tagNames, Boolean active, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        java.util.List<String> norm = normalize(tagNames);
        if (norm == null || norm.isEmpty()) {
            return linkRepository.searchUserLinksNoTags(user, query, active, pageable)
                .map(this::mapToLinkResponse);
        }
        return linkRepository.searchUserLinksWithTags(user, query, norm, active, pageable)
                .map(this::mapToLinkResponse);
    }

    @Transactional(readOnly = true)
    public LinkResponse getLinkById(String username, Long linkId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Link link = linkRepository.findByIdAndUser(linkId, user)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        return mapToLinkResponse(link);
    }

    @Transactional(readOnly = true)
    public java.util.List<String> listUserTagNames(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return linkRepository.findByUserOrderByDisplayOrderAsc(user).stream()
                .flatMap(l -> l.getTags().stream())
                .map(t -> t.getName())
                .distinct()
                .sorted()
                .toList();
    }

    @Caching(evict = {
        @CacheEvict(value = "publicProfiles", key = "#username"),
        @CacheEvict(value = "userLinks", key = "#username"),
        @CacheEvict(value = "analytics", key = "#username + '_overview'"),
        @CacheEvict(value = "analytics", key = "#username + '_detailed'"),
        @CacheEvict(value = "analytics", key = "#username + '_top_links'")
    })
    @Transactional
    public LinkResponse updateLink(String username, Long linkId, UpdateLinkRequest request) {
        log.info("Updating link {} for user: {}", linkId, username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Link link = linkRepository.findByIdAndUser(linkId, user)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        link.setTitle(sanitizeTitle(request.getTitle()));
        link.setUrl(sanitizeUrl(request.getUrl()));
        link.setDescription(sanitizeDescription(request.getDescription()));
        link.setStartAt(request.getStartAt());
        link.setEndAt(request.getEndAt());
        if (request.getTags() != null) {
            link.setTags(resolveTags(request.getTags()));
        }
        
        if (request.getIsActive() != null) {
            link.setIsActive(request.getIsActive());
        }

        if (request.getAlias() != null) {
            String trimmed = normalizeAlias(request.getAlias());
            if (trimmed.isEmpty()) {
                link.setAlias(null);
            } else if (!trimmed.equals(link.getAlias())) {
                Link existing = linkRepository.findByAlias(trimmed).orElse(null);
                if (existing != null && !existing.getId().equals(link.getId())) {
                    throw new RuntimeException("Alias already in use");
                }
                link.setAlias(trimmed);
            }
        }

        link = linkRepository.save(link);
        return mapToLinkResponse(link);
    }

    @Caching(evict = {
        @CacheEvict(value = "publicProfiles", key = "#username"),
        @CacheEvict(value = "userLinks", key = "#username"),
        @CacheEvict(value = "analytics", key = "#username + '_overview'"),
        @CacheEvict(value = "analytics", key = "#username + '_detailed'"),
        @CacheEvict(value = "analytics", key = "#username + '_top_links'")
    })
    @Transactional
    public void deleteLink(String username, Long linkId) {
        log.info("Deleting link {} for user: {}", linkId, username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Link link = linkRepository.findByIdAndUser(linkId, user)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        linkRepository.delete(link);
    }

    @Caching(evict = {
        @CacheEvict(value = "publicProfiles", key = "#username"),
        @CacheEvict(value = "userLinks", key = "#username"),
        @CacheEvict(value = "analytics", key = "#username + '_overview'"),
        @CacheEvict(value = "analytics", key = "#username + '_detailed'"),
        @CacheEvict(value = "analytics", key = "#username + '_top_links'")
    })
    @Transactional
    public void reorderLinks(String username, List<Long> linkIds) {
        log.info("Reordering links for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        for (int i = 0; i < linkIds.size(); i++) {
            Long linkId = linkIds.get(i);
            Link link = linkRepository.findByIdAndUser(linkId, user)
                    .orElseThrow(() -> new RuntimeException("Link not found: " + linkId));
            
            link.setDisplayOrder(i + 1);
            linkRepository.save(link);
        }
    }

    @Cacheable(value = "publicProfiles", key = "#username")
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username) {
        log.info("Fetching public profile from database for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Link> activeLinks = linkRepository.findActiveLinksForPublicProfile(username).stream()
                .filter(this::isWithinSchedule)
                .collect(Collectors.toList());

        List<PublicProfileResponse.PublicLinkResponse> linkResponses = activeLinks.stream()
                .map(link -> PublicProfileResponse.PublicLinkResponse.builder()
                        .id(link.getId())
                        .title(link.getTitle())
                        .url(link.getUrl())
                        .description(link.getDescription())
                        .displayOrder(link.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return PublicProfileResponse.builder()
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .profileImageUrl(user.getProfileImageUrl())
                .themePrimaryColor(user.getThemePrimaryColor())
                .themeAccentColor(user.getThemeAccentColor())
                .themeBackgroundColor(user.getThemeBackgroundColor())
                .themeTextColor(user.getThemeTextColor())
                .links(linkResponses)
                .build();
    }

    @Caching(evict = {
        @CacheEvict(value = "analytics", allEntries = true)
    })
    @Transactional
    public void trackLinkClick(Long linkId, String clientIp) {
        // Idempotency: suppress duplicate clicks from same IP for the same link within 5 seconds
        String idempotencyKey = "idemp:click:" + linkId + ":" + (clientIp != null ? clientIp : "unknown");
        Boolean exists = redisTemplate.hasKey(idempotencyKey);
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Duplicate click suppressed for link {} from ip {}", linkId, clientIp);
            return;
        }
        redisTemplate.opsForValue().set(idempotencyKey, "1", Duration.ofSeconds(5));

        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        link.setClickCount(link.getClickCount() + 1);
        linkRepository.save(link);
    }

    private boolean isWithinSchedule(Link link) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        if (link.getStartAt() != null && now.isBefore(link.getStartAt())) return false;
        if (link.getEndAt() != null && now.isAfter(link.getEndAt())) return false;
        return true;
    }

    private LinkResponse mapToLinkResponse(Link link) {
        return LinkResponse.builder()
                .id(link.getId())
                .title(link.getTitle())
                .url(link.getUrl())
                .description(link.getDescription())
                .isActive(link.getIsActive())
                .displayOrder(link.getDisplayOrder())
                .clickCount(link.getClickCount())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .alias(link.getAlias())
                .startAt(link.getStartAt())
                .endAt(link.getEndAt())
                .tags(link.getTags().stream().map(t -> t.getName()).sorted().toList())
                .build();
    }

    private java.util.Set<com.linkgrove.api.model.Tag> resolveTags(java.util.List<String> names) {
        java.util.Set<com.linkgrove.api.model.Tag> out = new java.util.HashSet<>();
        for (String raw : names) {
            if (raw == null) continue;
            final String normalized = collapseSeparators(raw.trim().toLowerCase());
            if (normalized.isEmpty()) continue;
            com.linkgrove.api.model.Tag tag = tagRepository.findByName(normalized).orElseGet(() -> {
                com.linkgrove.api.model.Tag t = com.linkgrove.api.model.Tag.builder().name(normalized).build();
                return tagRepository.save(t);
            });
            out.add(tag);
        }
        return out;
    }

    private java.util.List<String> normalize(java.util.List<String> names) {
        if (names == null) return java.util.List.of();
        return names.stream().filter(java.util.Objects::nonNull)
                .map(s -> collapseSeparators(s.trim().toLowerCase()))
                .filter(s -> !s.isEmpty()).distinct().toList();
    }

    private String sanitizeTitle(String title) {
        return title == null ? null : title.trim();
    }

    private String sanitizeUrl(String url) {
        return url == null ? null : url.trim();
    }

    private String sanitizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private String normalizeAlias(String alias) {
        if (alias == null) return null;
        String a = alias.trim();
        // collapse duplicate separators
        a = collapseSeparators(a);
        // strip leading/trailing separators (.-_)
        a = a.replaceAll("^[\\._-]+|[\\._-]+$", "");
        return a;
    }

    private String collapseSeparators(String s) {
        return s == null ? null : s.replaceAll("[\\._-]{2,}", "-");
    }
}
