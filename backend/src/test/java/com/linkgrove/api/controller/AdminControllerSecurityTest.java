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

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(TestSecurityConfig.class)
class AdminControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.repository.WebhookDeliveryRepository webhookDeliveryRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.RateLimitingConfig rateLimitingConfig;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.service.GeoIpService geoIpService;

    @Test
    @WithAnonymousUser
    void adminHealth_unauthenticated_is401() throws Exception {
        mockMvc.perform(get("/api/admin/health").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void adminHealth_userRole_is403() throws Exception {
        mockMvc.perform(get("/api/admin/health").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void adminHealth_adminRole_is200() throws Exception {
        mockMvc.perform(get("/api/admin/health").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void geoMetrics_unauthenticated_is401() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/geo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void geoMetrics_userRole_is403() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/geo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void geoMetrics_adminRole_is200() throws Exception {
        org.mockito.Mockito.when(geoIpService.isEnabled()).thenReturn(false);
        mockMvc.perform(get("/api/admin/metrics/geo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}


