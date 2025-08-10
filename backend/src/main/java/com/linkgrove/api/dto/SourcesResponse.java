package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SourcesResponse {
    private String username;
    private String period;
    private List<SourceStat> sources;
}


