package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateUsernameResponse {
    private String token;
    private String username;
}


