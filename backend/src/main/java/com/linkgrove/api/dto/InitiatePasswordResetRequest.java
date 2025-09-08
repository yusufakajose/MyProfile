package com.linkgrove.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitiatePasswordResetRequest {
    @NotBlank
    @Email
    private String email;
}


