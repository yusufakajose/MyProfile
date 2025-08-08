package com.linkgrove.api.service;

import com.linkgrove.api.dto.*;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.UserRepository;
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
                .title(request.getTitle())
                .url(request.getUrl())
                .description(request.getDescription())
                .user(user)
                .displayOrder(nextOrder)
                .isActive(true)
                .clickCount(0L)
                .build();

        if (request.getAlias() != null && !request.getAlias().isBlank()) {
            String alias = request.getAlias().trim();
            linkRepository.findByAlias(alias).ifPresent(existing -> {
                throw new RuntimeException("Alias already in use");
            });
            link.setAlias(alias);
        }

        Link saved = linkRepository.save(link);
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
    public Page<LinkResponse> getUserLinksPage(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return linkRepository.findByUserOrderByDisplayOrderAsc(user, pageable)
                .map(this::mapToLinkResponse);
    }

    @Transactional(readOnly = true)
    public Page<LinkResponse> searchUserLinks(String username, String query, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return linkRepository.searchUserLinks(user, query, pageable)
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

        link.setTitle(request.getTitle());
        link.setUrl(request.getUrl());
        link.setDescription(request.getDescription());
        
        if (request.getIsActive() != null) {
            link.setIsActive(request.getIsActive());
        }

        if (request.getAlias() != null) {
            String trimmed = request.getAlias().trim();
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

        List<Link> activeLinks = linkRepository.findActiveLinksForPublicProfile(username);

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
                .build();
    }
}
