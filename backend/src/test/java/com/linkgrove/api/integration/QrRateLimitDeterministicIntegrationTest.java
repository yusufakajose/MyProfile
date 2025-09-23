package com.linkgrove.api.integration;

import com.linkgrove.api.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "ratelimit.qr.maxRequests=1",
        "ratelimit.qr.windowSeconds=60"
})
class QrRateLimitDeterministicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void secondQrRequestWithinWindow_returns429WithRetryAfter() throws Exception {
        // First request should pass
        mockMvc.perform(get("/r/123/qr.png")
                        .param("size", "256")
                        .accept(MediaType.ALL))
                .andExpect(status().isOk());

        // Second immediate request should be rate limited
        mockMvc.perform(get("/r/123/qr.png")
                        .param("size", "256")
                        .accept(MediaType.ALL))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}


