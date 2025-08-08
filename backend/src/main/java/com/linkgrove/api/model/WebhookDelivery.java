package com.linkgrove.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_deliveries", indexes = {
        @Index(name = "idx_webhook_user_created", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String eventType; // e.g. link.click

    @Column(nullable = false, length = 500)
    private String targetUrl;

    @Column(nullable = false)
    private Integer attempt;

    @Column(nullable = false)
    private Integer statusCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String errorMessage;
}


