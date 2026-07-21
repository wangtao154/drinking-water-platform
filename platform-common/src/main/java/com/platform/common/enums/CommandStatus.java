package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MQTT 指令状态
 */
@Getter
@AllArgsConstructor
public enum CommandStatus {
    PENDING("待发送"),
    SENT("已发送"),
    EXECUTED("已执行"),
    TIMEOUT("超时"),
    FAILED("失败");

    private final String description;
}
