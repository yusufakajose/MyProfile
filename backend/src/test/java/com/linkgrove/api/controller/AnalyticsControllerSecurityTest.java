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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(TestSecurityConfig.class)
class AnalyticsControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.service.AnalyticsService analyticsService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.RateLimitingConfig rateLimitingConfig;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithAnonymousUser
    void analytics_overview_unauthenticated_is401() throws Exception {
        mockMvc.perform(get("/api/analytics/overview").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void analytics_overview_user_is200() throws Exception {
        mockMvc.perform(get("/api/analytics/overview").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void analytics_overview_admin_is200() throws Exception {
        mockMvc.perform(get("/api/analytics/overview").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}


