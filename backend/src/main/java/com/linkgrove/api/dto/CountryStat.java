package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CountryStat {
    private String country;
    private long clicks;
    private long uniqueVisitors;
}


