package com.platform.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQTT 配置 VO
 * 聚合所有 mqtt.* 配置项为一个对象，供前端统一展示
 */
@Data
public class MqttConfigVO implements Serializable {

    /** 主配置ID（mqtt.name 对应的 sys_config.id） */
    private Long id;

    /** 配置名称（用于显示） */
    private String name;

    /** MQTT Broker 地址 */
    private String broker;

    /** 客户端ID */
    private String clientId;

    /** 用户名 */
    private String username;

    /** 密码（脱敏显示） */
    private String password;

    /** 心跳间隔(秒) */
    private Integer keepAliveInterval = 60;

    /** 清除会话 */
    private Boolean cleanSession = false;

    /** 是否启用 */
    private Boolean enabled = true;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 运行状态：运行/停止（由 iot-service 实时上报） */
    private String runningStatus;

    /** 链路状态：连接/断开（由 iot-service 实时上报） */
    private String linkStatus;
}
