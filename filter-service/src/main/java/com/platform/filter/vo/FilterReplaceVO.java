package com.platform.filter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FilterReplaceVO {

    private Long id;

    private String deviceId;

    private String oldFilterId;

    private String newFilterId;

    private Long workOrderId;

    private Long replacedBy;

    private LocalDateTime replacedAt;

    private Integer oldUsedDuration;

    private Long oldUsedFlow;

    private String remark;

    private LocalDateTime createdAt;
}
