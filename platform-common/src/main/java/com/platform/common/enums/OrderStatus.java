package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING("待支付"),
    PAID("已支付"),
    CANCELLED("已取消"),
    REFUNDING("退款中"),
    REFUNDED("已退款"),
    ERROR("异常");

    private final String description;
}
