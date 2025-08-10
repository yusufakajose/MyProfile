package com.linkgrove.api.integration;

import com.linkgrove.api.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Basic integration test to verify the application starts correctly with Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class BasicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldStartApplicationSuccessfully() throws Exception {
        // Test that the application starts and basic endpoints work
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth service is running with database"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentPublicProfile() throws Exception {
        mockMvc.perform(get("/api/public/nonexistentuser"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForNonExistentRedirect() throws Exception {
        mockMvc.perform(get("/r/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForNonExistentPreview() throws Exception {
        mockMvc.perform(get("/r/99999/preview"))
                .andExpect(status().isNotFound());
    }
}
