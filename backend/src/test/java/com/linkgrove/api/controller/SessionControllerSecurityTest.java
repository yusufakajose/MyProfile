package com.linkgrove.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(TestSecurityConfig.class)
class SessionControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    com.linkgrove.api.repository.UserRepository userRepository;

    @MockitoBean
    com.linkgrove.api.repository.RefreshTokenRepository refreshTokenRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.RateLimitingConfig rateLimitingConfig;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.linkgrove.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = {"USER"})
    void listSessions_authenticated_is200() throws Exception {
        mockMvc.perform(get("/api/sessions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void revoke_authenticated_is204or200() throws Exception {
        mockMvc.perform(delete("/api/sessions/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());
    }
}


