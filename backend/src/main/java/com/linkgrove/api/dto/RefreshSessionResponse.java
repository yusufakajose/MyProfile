package com.linkgrove.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RefreshSessionResponse {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private String ip;
    private String userAgent;
    private LocalDateTime lastUsedAt;
}


