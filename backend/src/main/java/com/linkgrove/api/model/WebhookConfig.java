package com.linkgrove.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "webhook_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 128)
    private String secret; // used for HMAC signatures

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}


