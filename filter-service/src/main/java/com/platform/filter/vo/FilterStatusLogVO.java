package com.platform.filter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FilterStatusLogVO {

    private Long id;

    private String filterId;

    private String fromStatus;

    private String toStatus;

    private String operatorName;

    private String deviceId;

    private String remark;

    private LocalDateTime createdAt;
}
