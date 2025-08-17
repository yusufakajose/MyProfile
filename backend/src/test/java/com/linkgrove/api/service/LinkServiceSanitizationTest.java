package com.linkgrove.api.service;

import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.LinkResponse;
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

class LinkServiceSanitizationTest {

	private LinkRepository linkRepository;
	private UserRepository userRepository;
	private StringRedisTemplate redisTemplate;
	private TagRepository tagRepository;
	private QrPrewarmService qrPrewarmService;
	private LinkService linkService;

	@BeforeEach
	void setup() {
		linkRepository = mock(LinkRepository.class);
		userRepository = mock(UserRepository.class);
		redisTemplate = mock(StringRedisTemplate.class);
		tagRepository = mock(TagRepository.class);
		qrPrewarmService = mock(QrPrewarmService.class);
		linkService = new LinkService(linkRepository, userRepository, redisTemplate, tagRepository, qrPrewarmService);

		User u = User.builder().id(1L).username("alice").build();
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
		when(linkRepository.findMaxDisplayOrderForUser(u)).thenReturn(0);
	}

	@Test
	void trimsAndNormalizesAliasAndFieldsOnCreate() {
		CreateLinkRequest req = new CreateLinkRequest();
		req.setTitle("  Hello  ");
		req.setUrl("  https://example.com/page  ");
		req.setDescription("  Desc  ");
		req.setAlias("--My__Alias..--");

		ArgumentCaptor<Link> captor = ArgumentCaptor.forClass(Link.class);
		when(linkRepository.save(any(Link.class))).thenAnswer(inv -> {
			Link l = inv.getArgument(0);
			l.setId(42L);
			return l;
		});

		LinkResponse res = linkService.createLink("alice", req);
		assertNotNull(res);
		verify(linkRepository).save(captor.capture());
		Link saved = captor.getValue();
		assertEquals("Hello", saved.getTitle());
		assertEquals("https://example.com/page", saved.getUrl());
		assertEquals("Desc", saved.getDescription());
		// collapsed and trimmed separators: "My-Alias" (casing preserved)
		assertEquals("My-Alias", saved.getAlias());
	}
}


