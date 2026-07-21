package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MQTT 配置更新 DTO
 * 用于一次性更新所有 MQTT 相关配置项
 */
@Data
public class MqttConfigUpdateDTO {

    /** 配置名称（用于显示） */
    @NotBlank(message = "名称不能为空")
    private String name;

    /** MQTT Broker 地址（tcp://host:port） */
    @NotBlank(message = "MQTT地址不能为空")
    private String broker;

    /** 用户名（可选） */
    private String username;

    /** 密码（可选） */
    private String password;

    /** 客户端ID（可选，留空则使用默认值） */
    private String clientId;

    /** 心跳间隔（秒） */
    private Integer keepAliveInterval = 60;

    /** 清除会话 */
    private Boolean cleanSession = false;

    /** 是否启用 */
    private Boolean enabled = true;
}
