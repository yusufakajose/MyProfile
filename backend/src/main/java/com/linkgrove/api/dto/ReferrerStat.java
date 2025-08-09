package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferrerStat {
    private String referrerDomain;
    private long clicks;
    private long uniqueVisitors;
}


