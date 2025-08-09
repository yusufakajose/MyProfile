package com.linkgrove.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkVariantResponse {
    private Long id;
    private String title;
    private String url;
    private String description;
    private Integer weight;
    private Boolean isActive;
    private Long clickCount;
}


