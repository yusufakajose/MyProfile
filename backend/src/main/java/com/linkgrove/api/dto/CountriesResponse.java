package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CountriesResponse {
    private String username;
    private String period;
    private List<CountryStat> countries;
}


