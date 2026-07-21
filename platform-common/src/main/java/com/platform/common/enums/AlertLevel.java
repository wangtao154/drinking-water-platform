package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警级别
 */
@Getter
@AllArgsConstructor
public enum AlertLevel {
    WARNING("预警"),
    ALARM("报警");

    private final String description;
}
