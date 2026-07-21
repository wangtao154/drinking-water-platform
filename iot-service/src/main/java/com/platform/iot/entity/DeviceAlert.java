package com.platform.iot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_alert")
public class DeviceAlert extends BaseEntity {

    private String deviceId;

    private String sn;

    private String alertType;

    private String alertLevel;

    private String alertMessage;

    private String pushStatus;

    private String handledStatus;

    private Long autoWorkOrderId;

    private String relatedFilterId;

    private LocalDateTime triggeredAt;

    private LocalDateTime pushedAt;

    private LocalDateTime handledAt;
}
