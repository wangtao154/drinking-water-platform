package com.platform.filter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("filter_instance")
public class FilterInstance extends BaseEntity {

    private String filterId;

    private Long filterModelId;

    private LocalDate productionDate;

    private String productionBatch;

    private String qrCodeUrl;

    private String lifecycleStatus;

    private String currentDeviceId;

    private LocalDateTime installedAt;

    private Long installedBy;

    private Integer usedDuration;

    private Long usedFlow;

    private LocalDateTime scrappedAt;

    private String remark;
}
