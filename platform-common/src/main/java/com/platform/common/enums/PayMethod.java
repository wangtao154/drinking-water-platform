package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式
 */
@Getter
@AllArgsConstructor
public enum PayMethod {
    WECHAT_PAY("微信支付"),
    MANUAL_RECHARGE("后台手动充值");

    private final String description;
}
