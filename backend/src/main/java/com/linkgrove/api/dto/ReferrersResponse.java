package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferrersResponse {
    private String username;
    private String period;
    private List<ReferrerStat> referrers;
}


