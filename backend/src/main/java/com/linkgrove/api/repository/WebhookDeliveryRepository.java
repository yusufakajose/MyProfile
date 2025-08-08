package com.linkgrove.api.repository;

import com.linkgrove.api.model.User;
import com.linkgrove.api.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {
    List<WebhookDelivery> findTop20ByUserOrderByCreatedAtDesc(User user);
}


