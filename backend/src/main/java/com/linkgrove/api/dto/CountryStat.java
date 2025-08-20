package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountryStat {
    private String country;
    private long clicks;
    private long uniqueVisitors;
}


