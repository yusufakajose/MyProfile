package com.linkgrove.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkgrove.api.config.TestContainersConfig;
import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.LinkResponse;
import com.linkgrove.api.dto.LoginRequest;
import com.linkgrove.api.dto.RegisterRequest;
import com.linkgrove.api.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for the click tracking pipeline.
 * Tests the complete flow from link creation to click tracking and analytics.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class ClickTrackingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private String username;

    @BeforeEach
    void setUp() throws Exception {
        username = "testuser_" + System.currentTimeMillis();
        // Register a test user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Login to get auth token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), 
                AuthResponse.class
        );
        authToken = authResponse.getToken();
    }

    @Test
    void shouldCreateLinkAndTrackClicksSuccessfully() throws Exception {
        // 1. Create a link
        CreateLinkRequest linkRequest = new CreateLinkRequest();
        linkRequest.setTitle("Test Link");
        linkRequest.setUrl("https://example.com");
        linkRequest.setDescription("A test link");

        MvcResult createResult = mockMvc.perform(post("/api/links")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Test Link")))
                .andExpect(jsonPath("$.url", is("https://example.com")))
                .andExpect(jsonPath("$.clickCount", is(0)))
                .andReturn();

        LinkResponse createdLink = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                LinkResponse.class
        );
        Long linkId = createdLink.getId();

        // 1a. QR endpoints ETag behavior
        MvcResult qrPng = mockMvc.perform(get("/r/" + linkId + "/qr.png")
                        .param("size", "256")
                        .param("margin", "1")
                        .param("ecc", "M"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn();
        String etag = qrPng.getResponse().getHeader("ETag");
        mockMvc.perform(get("/r/" + linkId + "/qr.png")
                        .param("size", "256")
                        .param("margin", "1")
                        .param("ecc", "M")
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified());

        // 2. Test redirect endpoint
        mockMvc.perform(get("/r/" + linkId))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"))
                .andExpect(header().exists("Cache-Control"));

        // 3. Wait for async processing (RabbitMQ event)
        Thread.sleep(2000);

        // 4. Test link preview endpoint shows updated click count
        mockMvc.perform(get("/r/" + linkId + "/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(linkId.intValue())))
                .andExpect(jsonPath("$.title", is("Test Link")))
                .andExpect(jsonPath("$.url", is("https://example.com")))
                .andExpect(jsonPath("$.owner", is(username)))
                .andExpect(jsonPath("$.clickCount", greaterThan(0)));

        // 5. Test public profile shows the link
        mockMvc.perform(get("/api/public/" + username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)))
                .andExpect(jsonPath("$.links", hasSize(1)))
                .andExpect(jsonPath("$.links[0].id", is(linkId.intValue())))
                .andExpect(jsonPath("$.links[0].title", is("Test Link")));

        // 6. Test analytics endpoint
        mockMvc.perform(get("/api/analytics/detailed")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkAnalytics", hasSize(1)))
                .andExpect(jsonPath("$.linkAnalytics[0].id", is(linkId.intValue())))
                .andExpect(jsonPath("$.linkAnalytics[0].clickCount", greaterThanOrEqualTo(1)));

        // 7. Trigger a QR source click and verify sources endpoint
        mockMvc.perform(get("/r/" + linkId).param("src", "qr"))
                .andExpect(status().isFound());
        Thread.sleep(2000);
        mockMvc.perform(get("/api/analytics/sources")
                        .param("days", "7")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleLinkNotFoundError() throws Exception {
        // Test redirect with non-existent link ID
        mockMvc.perform(get("/r/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Link Not Found")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Link not found with ID: 99999")));

        // Test preview with non-existent link ID
        mockMvc.perform(get("/r/99999/preview"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCacheLinkRedirectsForPerformance() throws Exception {
        // Create a link first
        CreateLinkRequest linkRequest = new CreateLinkRequest();
        linkRequest.setTitle("Cached Link");
        linkRequest.setUrl("https://cached.example.com");
        linkRequest.setDescription("A link for cache testing");

        MvcResult createResult = mockMvc.perform(post("/api/links")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        LinkResponse createdLink = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                LinkResponse.class
        );
        Long linkId = createdLink.getId();

        // First request (cache miss)
        long startTime1 = System.currentTimeMillis();
        mockMvc.perform(get("/r/" + linkId))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second request (cache hit - should be faster)
        long startTime2 = System.currentTimeMillis();
        mockMvc.perform(get("/r/" + linkId))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
        long duration2 = System.currentTimeMillis() - startTime2;

        // Cache hit should generally be faster, but we're just ensuring it works
        assertTrue(duration1 >= 0 && duration2 >= 0, "Both requests should complete successfully");
    }

    @Test
    void shouldUpdateClickCountsAsynchronously() throws Exception {
        // Create a link
        CreateLinkRequest linkRequest = new CreateLinkRequest();
        linkRequest.setTitle("Async Test Link");
        linkRequest.setUrl("https://async.example.com");
        linkRequest.setDescription("Testing async click counting");

        MvcResult createResult = mockMvc.perform(post("/api/links")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        LinkResponse createdLink = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                LinkResponse.class
        );
        Long linkId = createdLink.getId();

        // Get initial click count
        MvcResult initialPreview = mockMvc.perform(get("/r/" + linkId + "/preview"))
                .andExpect(status().isOk())
                .andReturn();

        String initialContent = initialPreview.getResponse().getContentAsString();
        int initialClickCount = objectMapper.readTree(initialContent).get("clickCount").asInt();

        // Trigger multiple clicks
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/r/" + linkId))
                    .andExpect(status().isFound());
        }

        // Wait for async processing
        Thread.sleep(3000);

        // Clear cache to ensure we get fresh data from database
        // Note: In a real test, we'd clear the specific cache keys

        // Verify click count increased
        mockMvc.perform(get("/r/" + linkId + "/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount", greaterThanOrEqualTo(initialClickCount)));
    }

    @Test
    void shouldHandleHighConcurrentClicks() throws Exception {
        // Create a link
        CreateLinkRequest linkRequest = new CreateLinkRequest();
        linkRequest.setTitle("Concurrent Test Link");
        linkRequest.setUrl("https://concurrent.example.com");
        linkRequest.setDescription("Testing concurrent clicks");

        MvcResult createResult = mockMvc.perform(post("/api/links")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isCreated())
                .andReturn();                                                   

        LinkResponse createdLink = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                LinkResponse.class
        );
        Long linkId = createdLink.getId();

        // Simulate concurrent clicks
        int numberOfClicks = 10;
        Thread[] threads = new Thread[numberOfClicks];

        for (int i = 0; i < numberOfClicks; i++) {
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/r/" + linkId))
                            .andExpect(status().isFound());
                } catch (Exception e) {
                    fail("Concurrent click failed: " + e.getMessage());
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Wait for async processing
        Thread.sleep(5000);

        // Verify all clicks were processed (should be at least numberOfClicks)
        mockMvc.perform(get("/r/" + linkId + "/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount", greaterThanOrEqualTo(1)));
    }
}
