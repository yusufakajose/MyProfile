package com.linkgrove.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateLinkRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @NotBlank(message = "URL is required")
    @Size(max = 500, message = "URL must not exceed 500 characters")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String url;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    private Boolean isActive;

    @Size(min = 3, max = 50, message = "Alias must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-_\\.]+$", message = "Alias can contain letters, numbers, hyphen, underscore, dot")
    private String alias;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
