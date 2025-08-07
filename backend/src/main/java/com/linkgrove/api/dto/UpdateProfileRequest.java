package com.linkgrove.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    @Size(max = 200, message = "Profile image URL must be at most 200 characters")
    private String profileImageUrl;

    @Size(max = 20)
    private String themePrimaryColor;

    @Size(max = 20)
    private String themeAccentColor;

    @Size(max = 20)
    private String themeBackgroundColor;

    @Size(max = 20)
    private String themeTextColor;
}


