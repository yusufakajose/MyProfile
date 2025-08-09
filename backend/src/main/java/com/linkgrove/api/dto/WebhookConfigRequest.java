package com.linkgrove.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

/**
 * Request DTO for webhook configuration updates.
 */
@Data
public class WebhookConfigRequest {

    @NotBlank(message = "Webhook URL is required")
    @Size(max = 500, message = "Webhook URL must be at most 500 characters")
    @URL(message = "Webhook URL must be a valid URL")
    @Pattern(regexp = "^https?://.+", message = "Webhook URL must start with http:// or https://")
    private String url;

    private Boolean isActive;
}


