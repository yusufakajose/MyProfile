package com.linkgrove.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private String themePrimaryColor;
    private String themeAccentColor;
    private String themeBackgroundColor;
    private String themeTextColor;
}


