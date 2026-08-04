package com.platform.ai.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeviceMetadata {

    private Long id;
    private String deviceId;
    private String sn;
    private Long modelId;
    private String modelName;
    private Boolean online;
    private String lifecycleStatus;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
}
