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

    @Query("select count(d) from WebhookDelivery d")
    long countAll();

    @Query("select count(d) from WebhookDelivery d where d.statusCode between 200 and 299")
    long countSuccess();

    @Query("select count(d) from WebhookDelivery d where d.deadLettered = true")
    long countDeadLettered();

    @Query("select d.targetUrl, count(d) from WebhookDelivery d group by d.targetUrl order by count(d) desc")
    List<Object[]> countByTargetUrl();

    // Time-bounded metrics
    @Query("select count(d) from WebhookDelivery d where d.createdAt >= :since")
    long countAllSince(@Param("since") java.time.LocalDateTime since);

    @Query("select count(d) from WebhookDelivery d where d.statusCode between 200 and 299 and d.createdAt >= :since")
    long countSuccessSince(@Param("since") java.time.LocalDateTime since);

    @Query("select count(d) from WebhookDelivery d where d.deadLettered = true and d.createdAt >= :since")
    long countDeadLetteredSince(@Param("since") java.time.LocalDateTime since);

    // DLQ counts per destination
    @Query("select d.targetUrl, count(d) from WebhookDelivery d where d.deadLettered = true group by d.targetUrl order by count(d) desc")
    List<Object[]> countDeadLetteredByTargetUrl();

    @Query("select d.targetUrl, count(d) from WebhookDelivery d where d.deadLettered = true and d.createdAt >= :since group by d.targetUrl order by count(d) desc")
    List<Object[]> countDeadLetteredByTargetUrlSince(@Param("since") java.time.LocalDateTime since);
}


