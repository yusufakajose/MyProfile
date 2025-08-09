package com.linkgrove.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStat {
    private String deviceType;
    private long clicks;
    private long uniqueVisitors;
}


