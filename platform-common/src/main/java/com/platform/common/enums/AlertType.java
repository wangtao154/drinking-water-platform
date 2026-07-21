package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警类型
 */
@Getter
@AllArgsConstructor
public enum AlertType {
    WATER_QUALITY("水质异常"),
    DEVICE_FAULT("设备故障"),
    WATER_LEAK("漏水"),
    WATER_SHORTAGE("缺水"),
    LOW_VOLTAGE("电压低"),
    FILTER_EXPIRE("滤芯到期"),
    FLOW_EXPIRE("流量到期"),
    RENT_EXPIRE("租期到期");

    private final String description;
}
