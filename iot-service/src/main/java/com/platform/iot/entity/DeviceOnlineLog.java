package com.platform.iot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_online_log")
public class DeviceOnlineLog extends BaseEntity {

    private String sn;

    private String deviceId;

    private String eventType;

    private LocalDateTime occurredAt;
}
