package com.platform.device.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceModelVO {

    private Long id;

    private String modelCode;

    private String modelName;

    private String category;

    private String description;

    private String status;

    private String filterConfig;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
