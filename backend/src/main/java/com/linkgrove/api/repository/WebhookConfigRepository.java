package com.linkgrove.api.repository;

import com.linkgrove.api.model.User;
import com.linkgrove.api.model.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {
    Optional<WebhookConfig> findFirstByUserAndIsActiveTrue(User user);
}


