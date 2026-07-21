package com.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备激活事件模型
 * 当客户绑定设备成功后，device-service 发布此事件
 * iot-service 消费后自动为该设备订阅 MQTT 主题
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceActivatedEvent implements Serializable {

    /**
     * 网关序列号（MQTT ClientID）
     */
    private String sn;

    /**
     * 平台设备ID
     */
    private String deviceId;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 激活时间（毫秒时间戳）
     */
    private Long activatedAt;
}
