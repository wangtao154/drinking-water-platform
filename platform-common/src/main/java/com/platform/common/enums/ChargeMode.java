package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计费模式
 */
@Getter
@AllArgsConstructor
public enum ChargeMode {
    FLOW_BASED("流量计费"),
    MONTHLY_RENT("包月租赁"),
    PACKAGE_RECHARGE("套餐充值"),
    SHARED("共享计费");

    private final String description;
}
