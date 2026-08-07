package com.platform.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备上下线状态变更事件
 *
 * 由 iot-service 在收到 MQTT LWT 消息时发布到 RabbitMQ，
 * push-service 消费后向已绑定的管理员发送公众号模板消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备 SN（如 j082438） */
    private String sn;

    /** 设备业务编号（如 0002），用作模板的「工单编号」字段 */
    private String deviceId;

    /** 设备型号名称（可选，用于通知文案） */
    private String modelName;

    /** 设备归属经销商名称（可选） */
    private String dealerName;

    /** 状态：ONLINE / OFFLINE */
    private String status;

    /** 状态变更时间 */
    private LocalDateTime occurredAt;
}