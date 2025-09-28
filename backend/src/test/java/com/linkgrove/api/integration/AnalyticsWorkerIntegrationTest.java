package com.linkgrove.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkgrove.api.config.RabbitMQConfig;
import com.linkgrove.api.config.TestContainersConfig;
import com.linkgrove.api.dto.AuthResponse;
import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.LinkResponse;
import com.linkgrove.api.dto.LoginRequest;
import com.linkgrove.api.dto.RegisterRequest;
import com.linkgrove.api.event.LinkClickEvent;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.LinkDeviceDailyAggregate;
import com.linkgrove.api.model.LinkReferrerDailyAggregate;
import com.linkgrove.api.model.LinkSourceDailyAggregate;
import com.linkgrove.api.repository.LinkClickDailyAggregateRepository;
import com.linkgrove.api.repository.LinkDeviceDailyAggregateRepository;
import com.linkgrove.api.repository.LinkReferrerDailyAggregateRepository;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.LinkSourceDailyAggregateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class AnalyticsWorkerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkClickDailyAggregateRepository aggregateRepository;

    @Autowired
    private LinkReferrerDailyAggregateRepository referrerRepository;

    @Autowired
    private LinkDeviceDailyAggregateRepository deviceRepository;

    @Autowired
    private LinkSourceDailyAggregateRepository sourceRepository;

    private String authToken;
    private String username;
    private Long linkId;
    private String linkUrl;

    @BeforeEach
    void setUp() throws Exception {
        username = "worker_it_" + UUID.randomUUID();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword(TestPasswordUtil.strong());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(TestPasswordUtil.strong());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        authToken = authResponse.getToken();

        CreateLinkRequest linkRequest = new CreateLinkRequest();
        linkRequest.setTitle("Worker IT Link");
        linkRequest.setUrl("https://example.com/worker-it");
        linkRequest.setDescription("Worker integration test");

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title", is("Worker IT Link")))
                .andReturn();

        LinkResponse createdLink = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                LinkResponse.class
        );
        linkId = createdLink.getId();
        linkUrl = createdLink.getUrl();
    }

    @Test
    void workerUpsertHappyPath_persistsAggregates() throws Exception {
        String sessionId = "sess-" + UUID.randomUUID();
        Instant clickedAt = Instant.now().minusSeconds(5);

        LinkClickEvent event = LinkClickEvent.builder()
                .linkId(linkId)
                .username(username)
                .clickedAt(clickedAt)
                .clientIp("1.1.1.1")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 13_2_1) Chrome/125.0.0.0 Safari/537.36")
                .referrer("https://news.ycombinator.com/item?id=123")
                .sessionId(sessionId)
                .targetUrl(linkUrl)
                .source("qr")
                .utmMedium("social")
                .requestId(UUID.randomUUID().toString())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LINK_CLICK_EXCHANGE,
                RabbitMQConfig.LINK_CLICK_ROUTING_KEY,
                event
        );

        LocalDate day = clickedAt.atZone(ZoneOffset.UTC).toLocalDate();

        Link updatedLink = await("link click count", () -> linkRepository.findById(linkId).orElse(null),
                link -> link != null && link.getClickCount() >= 1L);
        assertNotNull(updatedLink);
        assertEquals(1L, updatedLink.getClickCount());

        List<com.linkgrove.api.model.LinkClickDailyAggregate> aggregates = await(
                "daily aggregate",
                () -> aggregateRepository.findRangeForLink(username, linkId, day, day),
                rows -> !rows.isEmpty() && rows.get(0).getClicks() >= 1L
        );
        com.linkgrove.api.model.LinkClickDailyAggregate aggregate = aggregates.get(0);
        assertEquals(1L, aggregate.getClicks());
        assertEquals(1L, aggregate.getUniqueVisitors());

        List<LinkSourceDailyAggregate> sources = await(
                "source aggregate",
                () -> sourceRepository.findRangeForLink(username, linkId, day, day),
                rows -> rows.stream().anyMatch(row -> "qr".equals(row.getSource()))
        );
        LinkSourceDailyAggregate sourceRow = sources.stream()
                .filter(row -> "qr".equals(row.getSource()))
                .findFirst()
                .orElseThrow();
        assertEquals(1L, sourceRow.getClicks());
        assertEquals(1L, sourceRow.getUniqueVisitors());

        List<LinkReferrerDailyAggregate> referrers = await(
                "referrer aggregate",
                () -> referrerRepository.findRange(username, day, day),
                rows -> rows.stream().anyMatch(row -> row.getLink().getId().equals(linkId))
        );
        LinkReferrerDailyAggregate refRow = referrers.stream()
                .filter(row -> row.getLink().getId().equals(linkId))
                .findFirst()
                .orElseThrow();
        assertEquals("news.ycombinator.com", refRow.getReferrerDomain());
        assertEquals(1L, refRow.getClicks());
        assertEquals(1L, refRow.getUniqueVisitors());

        List<LinkDeviceDailyAggregate> devices = await(
                "device aggregate",
                () -> deviceRepository.findRange(username, day, day),
                rows -> rows.stream().anyMatch(row -> row.getLink().getId().equals(linkId))
        );
        LinkDeviceDailyAggregate deviceRow = devices.stream()
                .filter(row -> row.getLink().getId().equals(linkId))
                .findFirst()
                .orElseThrow();
        assertEquals("desktop", deviceRow.getDeviceType());
        assertEquals(1L, deviceRow.getClicks());
        assertEquals(1L, deviceRow.getUniqueVisitors());
    }

    private <T> T await(String description, Supplier<T> supplier, Predicate<T> condition) throws InterruptedException {
        for (int attempt = 0; attempt < 40; attempt++) {
            T value = supplier.get();
            if (condition.test(value)) {
                return value;
            }
            Thread.sleep(250);
        }
        fail("Timed out waiting for " + description);
        return null;
    }
}


