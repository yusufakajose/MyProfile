package com.linkgrove.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkgrove.api.config.JwtAuthenticationFilter;
import com.linkgrove.api.config.RateLimitingConfig;
import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.exception.GlobalExceptionHandler;
import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.LinkVariantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LinkController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class LinkControllerSecurityBoundaryTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LinkService linkService;

    @MockitoBean
    LinkVariantService linkVariantService;

    // Provide a mock so SecurityConfig can wire the filter without needing JwtUtil/UserRepository
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    // Mock rate limiter to avoid requiring Redis in slice
    @MockitoBean
    RateLimitingConfig rateLimitingConfig;

    @Test
    @WithAnonymousUser
    void createLink_unauthenticated_returns401() throws Exception {
        CreateLinkRequest req = new CreateLinkRequest();
        req.setTitle("My Link");
        req.setUrl("https://example.com");
        req.setDescription("d");
        req.setAlias("my-alias");

        mockMvc.perform(post("/api/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }
}


