package com.platform.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_binding")
public class DeviceBinding extends BaseEntity {

    private String deviceId;

    private Long customerId;

    private String bindType;

    private LocalDateTime bindAt;

    private LocalDateTime unbindAt;

    private String status;

    private Long activatedPackageId;
}
