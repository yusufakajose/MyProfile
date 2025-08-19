package com.linkgrove.api.service;

import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.LinkResponse;
import com.linkgrove.api.dto.UpdateLinkRequest;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.TagRepository;
import com.linkgrove.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LinkServiceCoreTest {

    LinkRepository linkRepository;
    UserRepository userRepository;
    StringRedisTemplate redisTemplate;
    TagRepository tagRepository;
    QrPrewarmService qrPrewarmService;
    LinkService linkService;

    @BeforeEach
    void setup() {
        linkRepository = mock(LinkRepository.class);
        userRepository = mock(UserRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        tagRepository = mock(TagRepository.class);
        qrPrewarmService = mock(QrPrewarmService.class);
        linkService = new LinkService(linkRepository, userRepository, redisTemplate, tagRepository, qrPrewarmService);
    }

    @Test
    void createLink_assignsNextDisplayOrder_andCallsPrewarm() {
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(linkRepository.findMaxDisplayOrderForUser(user)).thenReturn(3);
        when(linkRepository.findByAlias("my-alias")).thenReturn(Optional.empty());

        ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
        when(linkRepository.save(linkCaptor.capture())).thenAnswer(inv -> {
            Link l = linkCaptor.getValue();
            l.setId(10L);
            return l;
        });

        CreateLinkRequest req = new CreateLinkRequest();
        req.setTitle("  Title  ");
        req.setUrl(" https://e.com ");
        req.setDescription(" d ");
        req.setAlias("--my--alias--");

        LinkResponse resp = linkService.createLink("alice", req);

        Link saved = linkCaptor.getValue();
        assertEquals(4, saved.getDisplayOrder());
        assertEquals("Title", saved.getTitle());
        assertEquals("https://e.com", saved.getUrl());
        assertEquals("d", saved.getDescription());
        assertEquals("my-alias", saved.getAlias());
        assertEquals(10L, resp.getId());

        verify(qrPrewarmService, times(1)).onLinkCreatedOrUpdated(any(Link.class));
    }

    @Test
    void createLink_aliasAlreadyInUse_throws() {
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(linkRepository.findMaxDisplayOrderForUser(user)).thenReturn(null);
        when(linkRepository.findByAlias("taken")).thenReturn(Optional.of(Link.builder().id(2L).build()));

        CreateLinkRequest req = new CreateLinkRequest();
        req.setTitle("T");
        req.setUrl("https://e.com");
        req.setAlias("taken");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> linkService.createLink("alice", req));
        assertTrue(ex.getMessage().toLowerCase().contains("alias"));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void updateLink_normalizesAndSaves_andChecksAliasUniqueness() {
        User user = User.builder().id(1L).username("alice").build();
        Link existing = Link.builder().id(5L).title("Old").url("https://old").description("")
                .user(user).displayOrder(1).isActive(true).clickCount(0L).alias("old")
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(linkRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(existing));
        when(linkRepository.findByAlias("new-alias")).thenReturn(Optional.empty());
        when(linkRepository.save(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLinkRequest req = new UpdateLinkRequest();
        req.setTitle(" New ");
        req.setUrl(" https://new ");
        req.setDescription(" d ");
        req.setIsActive(Boolean.FALSE);
        req.setAlias("..new--alias..");

        LinkResponse resp = linkService.updateLink("alice", 5L, req);
        assertEquals("New", resp.getTitle());
        assertEquals("https://new", resp.getUrl());
        assertEquals(false, resp.getIsActive());
        assertEquals("new-alias", resp.getAlias());
        verify(linkRepository, times(1)).save(any(Link.class));
    }

    @Test
    void reorderLinks_setsSequentialDisplayOrder() {
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Link l1 = Link.builder().id(1L).user(user).displayOrder(99).build();
        Link l2 = Link.builder().id(2L).user(user).displayOrder(99).build();
        Link l3 = Link.builder().id(3L).user(user).displayOrder(99).build();
        when(linkRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(l1));
        when(linkRepository.findByIdAndUser(2L, user)).thenReturn(Optional.of(l2));
        when(linkRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(l3));

        linkService.reorderLinks("alice", java.util.List.of(3L, 1L, 2L));

        assertEquals(1, l3.getDisplayOrder());
        assertEquals(2, l1.getDisplayOrder());
        assertEquals(3, l2.getDisplayOrder());
        verify(linkRepository, times(3)).save(any(Link.class));
    }
}


