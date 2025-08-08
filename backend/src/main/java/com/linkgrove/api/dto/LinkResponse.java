package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkResponse {

    private Long id;
    private String title;
    private String url;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String alias;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
