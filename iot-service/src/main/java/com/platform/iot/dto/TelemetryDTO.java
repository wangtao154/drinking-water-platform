package com.platform.iot.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TelemetryDTO {
    private String deviceSn;
    private String timestamp;
    private Map<String, Object> points;
    private boolean online;
}
