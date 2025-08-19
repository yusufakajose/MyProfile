package com.linkgrove.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.LinkResponse;
import com.linkgrove.api.exception.GlobalExceptionHandler;
import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.LinkVariantService;
import com.linkgrove.api.config.JwtAuthenticationFilter;
import com.linkgrove.api.config.RateLimitingConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.junit.jupiter.api.Disabled("Covered by standalone happy-path; keeping disabled to avoid duplication")
@WebMvcTest(controllers = LinkController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestMvcAuthConfig.class})
class LinkControllerWebMvcHappyPathTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LinkService linkService;

    @MockitoBean
    LinkVariantService linkVariantService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    RateLimitingConfig rateLimitingConfig;

    @MockitoBean
    StringRedisTemplate stringRedisTemplate;

    @Test
    void createLink_happyPath_returns201AndBody() throws Exception {
        CreateLinkRequest req = new CreateLinkRequest();
        req.setTitle("My Link");
        req.setUrl("https://example.com");
        req.setDescription("d");
        req.setAlias("my-alias");

        LinkResponse stub = LinkResponse.builder()
                .id(42L)
                .title("My Link")
                .url("https://example.com")
                .description("d")
                .alias("my-alias")
                .isActive(true)
                .displayOrder(1)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tags(java.util.List.of("tag1", "tag2"))
                .build();

        Mockito.when(linkService.createLink(Mockito.eq("alice"), Mockito.any(CreateLinkRequest.class)))
                .thenReturn(stub);

        mockMvc.perform(post("/api/links")
                        .with(csrf())
                        .principal(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("alice", "N/A", org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(42)))
                .andExpect(jsonPath("$.title", is("My Link")))
                .andExpect(jsonPath("$.url", is("https://example.com")))
                .andExpect(jsonPath("$.alias", is("my-alias")))
                .andExpect(jsonPath("$.isActive", is(true)));
    }
}


