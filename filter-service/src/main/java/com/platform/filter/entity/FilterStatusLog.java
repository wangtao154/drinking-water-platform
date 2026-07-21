package com.platform.filter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("filter_status_log")
public class FilterStatusLog {

    private Long id;

    private String filterId;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String operatorName;

    private String deviceId;

    private String remark;

    private LocalDateTime createdAt;
}
