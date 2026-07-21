package com.platform.filter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("filter_replace_log")
public class FilterReplaceLog {

    private Long id;

    private String deviceId;

    private String oldFilterId;

    private String newFilterId;

    private Long workOrderId;

    private Long replacedBy;

    private LocalDateTime replacedAt;

    private Integer oldUsedDuration;

    private Long oldUsedFlow;

    private String photoUrls;

    private String remark;

    private LocalDateTime createdAt;
}
