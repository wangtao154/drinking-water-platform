package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备生命周期状态
 */
@Getter
@AllArgsConstructor
public enum DeviceLifecycleStatus {
    REGISTERED("已登记（未激活）"),
    ALLOCATED("已配发（在库）"),
    PENDING_INSTALL("待安装"),
    ACTIVATED_ONLINE("已激活（在线）"),
    ACTIVATED_OFFLINE("已激活（离线）"),
    RETURNED("已退机"),
    SCRAPPED("已报废");

    private final String description;
}
