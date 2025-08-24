package com.linkgrove.api.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Component
public class WebhookHttpClient {

    private final RestTemplate restTemplate;

    public WebhookHttpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @TimeLimiter(name = "webhook")
    @CircuitBreaker(name = "webhook")
    @Retry(name = "webhook")
    public CompletableFuture<ResponseEntity<String>> postAsync(String url, HttpEntity<String> entity) {
        return CompletableFuture.supplyAsync(() -> restTemplate.postForEntity(url, entity, String.class));
    }
}


