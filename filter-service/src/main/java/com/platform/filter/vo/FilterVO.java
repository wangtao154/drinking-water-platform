package com.platform.filter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FilterVO {

    private Long id;

    private String filterId;

    private Long filterModelId;

    private String modelName;

    private String lifecycleStatus;

    private String currentDeviceId;

    private LocalDateTime installedAt;

    private LocalDateTime scrappedAt;

    private Integer usedDuration;

    private Long usedFlow;

    private Integer remainPercentage;

    private String qrCodeUrl;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
