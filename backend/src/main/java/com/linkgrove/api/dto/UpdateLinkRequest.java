package com.linkgrove.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import com.linkgrove.api.validation.StartBeforeEnd;
import com.linkgrove.api.validation.AliasNotReserved;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@StartBeforeEnd
public class UpdateLinkRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @NotBlank(message = "URL is required")
    @Size(max = 500, message = "URL must not exceed 500 characters")
    @URL(message = "URL must be a valid URL")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String url;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    private Boolean isActive;

    @Size(min = 3, max = 50, message = "Alias must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9](?:[a-zA-Z0-9-_\\.]{1,48}[a-zA-Z0-9])$", message = "Alias must be alphanumeric and may include - _ . in the middle")
    @AliasNotReserved
    private String alias;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private java.util.List<String> tags; // names
}
