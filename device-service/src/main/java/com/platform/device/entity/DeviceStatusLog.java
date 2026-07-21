package com.platform.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_status_log")
public class DeviceStatusLog extends BaseEntity {

    private String deviceId;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String operatorName;

    private String remark;
}
