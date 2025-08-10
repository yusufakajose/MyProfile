package com.linkgrove.api.service;

import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.LinkClickDailyAggregateRepository;
import com.linkgrove.api.repository.LinkRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QrPrewarmServiceTest {

    private QrCodeService qrCodeService;
    private LinkRepository linkRepository;
    private LinkClickDailyAggregateRepository clickAggRepo;
    private StringRedisTemplate redisTemplate;
    private SimpleMeterRegistry meterRegistry;

    private QrPrewarmService prewarmService;

    @BeforeEach
    void setup() throws Exception {
        qrCodeService = mock(QrCodeService.class);
        linkRepository = mock(LinkRepository.class);
        clickAggRepo = mock(LinkClickDailyAggregateRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any())).thenReturn(Boolean.TRUE);
        meterRegistry = new SimpleMeterRegistry();

        prewarmService = new QrPrewarmService(qrCodeService, linkRepository, clickAggRepo, redisTemplate, meterRegistry);

        // Set enabled=true via reflection since @Value is not processed in plain unit test
        var enabledField = QrPrewarmService.class.getDeclaredField("enabled");
        enabledField.setAccessible(true);
        enabledField.set(prewarmService, true);

        var lockTtlField = QrPrewarmService.class.getDeclaredField("lockTtlSeconds");
        lockTtlField.setAccessible(true);
        lockTtlField.set(prewarmService, 60L);
    }

    @Test
    void prewarmOnLinkCreateGeneratesPngAndSvgPresets() {
        Link link = Link.builder().id(123L).isActive(true).user(User.builder().username("u").build()).build();

        prewarmService.onLinkCreatedOrUpdated(link);

        // 2 sizes x 2 ECC x 1 format each ⇒ at least 4 calls per format
        verify(qrCodeService, atLeast(4)).generatePng(contains("/r/123"), anyInt(), anyInt(), any(), any(), any(), any());
        verify(qrCodeService, atLeast(4)).generateSvg(contains("/r/123"), anyInt(), anyInt(), any(), any(), any());

        // Capture one call to ensure path formatting is as expected
        ArgumentCaptor<String> contentCap = ArgumentCaptor.forClass(String.class);
        verify(qrCodeService, atLeastOnce()).generatePng(contentCap.capture(), anyInt(), anyInt(), any(), any(), any(), any());
        assertThat(contentCap.getAllValues().get(0)).startsWith("/r/");
    }
}


