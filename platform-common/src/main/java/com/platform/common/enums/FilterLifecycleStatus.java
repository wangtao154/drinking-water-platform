package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 滤芯生命周期状态
 */
@Getter
@AllArgsConstructor
public enum FilterLifecycleStatus {
    IN_STOCK("在库"),
    PENDING_INSTALL("待安装"),
    IN_USE("使用中"),
    SCRAPPED("已报废");

    private final String description;
}
