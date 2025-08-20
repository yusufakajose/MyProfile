package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourcesResponse {
    private String username;
    private String period;
    private List<SourceStat> sources;
}


