package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SourceStat {
    private String source;
    private long clicks;
    private long uniqueVisitors;
}


