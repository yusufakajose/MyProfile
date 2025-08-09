package com.linkgrove.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LinkVariantRequest {
    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 500)
    @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
    private String url;

    @Size(max = 500)
    private String description;

    @Min(0)
    private Integer weight;

    private Boolean isActive;
}


