package com.linkgrove.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PerformPasswordResetRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}


