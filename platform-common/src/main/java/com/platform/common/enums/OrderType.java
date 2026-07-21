package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单类型
 */
@Getter
@AllArgsConstructor
public enum OrderType {
    RECHARGE("充值"),
    BUY_WATER("购水");

    private final String description;
}
