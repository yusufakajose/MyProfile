package com.linkgrove.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.LinkVariantService;
import io.micrometer.tracing.Tracer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LinkController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(TestSecurityConfig.class)
class LinkControllerRoleSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LinkService linkService;

    @MockitoBean
    LinkVariantService linkVariantService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.RateLimitingConfig rateLimitingConfig;

    @MockitoBean
    Tracer tracer;

    @Test
    @WithAnonymousUser
    void listLinks_unauthenticated_is401() throws Exception {
        mockMvc.perform(get("/api/links").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void listLinks_user_is200() throws Exception {
        mockMvc.perform(get("/api/links").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void listLinks_admin_is200() throws Exception {
        mockMvc.perform(get("/api/links").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}


