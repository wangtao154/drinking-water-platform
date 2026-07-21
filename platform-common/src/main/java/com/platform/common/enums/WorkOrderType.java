package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单类型
 */
@Getter
@AllArgsConstructor
public enum WorkOrderType {
    INSTALL_APPOINTMENT("预约装机"),
    REPAIR("维修"),
    REMOVE("拆机"),
    RELOCATE("迁机"),
    FILTER_REPLACE("滤芯更换");

    private final String description;
}
