package com.linkgrove.api.repository;

import com.linkgrove.api.model.User;
import com.linkgrove.api.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {
    List<WebhookDelivery> findTop20ByUserOrderByCreatedAtDesc(User user);
    List<WebhookDelivery> findTop50ByUserAndStatusCodeLessThanOrderByCreatedAtDesc(User user, int statusCodeThreshold);

    @Query("select d from WebhookDelivery d where d.deadLettered = false and d.nextAttemptAt is not null and d.nextAttemptAt <= :now order by d.nextAttemptAt asc")
    List<WebhookDelivery> findDueRetries(@Param("now") java.time.LocalDateTime now);

    List<WebhookDelivery> findTop50ByUserAndDeadLetteredTrueOrderByCreatedAtDesc(User user);
}


