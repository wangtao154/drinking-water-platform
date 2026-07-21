package com.platform.workorder.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作人类型
 */
@Getter
@AllArgsConstructor
public enum OperatorType {

    ADMIN("管理员"),
    DEALER("经销商"),
    WORKER("运维人员"),
    CUSTOMER("客户");

    private final String description;
}
