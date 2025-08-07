package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileResponse {

    private String username;
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private String themePrimaryColor;
    private String themeAccentColor;
    private String themeBackgroundColor;
    private String themeTextColor;
    private List<PublicLinkResponse> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicLinkResponse {
        private Long id;
        private String title;
        private String url;
        private String description;
        private Integer displayOrder;
    }
}
