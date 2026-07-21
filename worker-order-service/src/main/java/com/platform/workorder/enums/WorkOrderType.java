package com.platform.workorder.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单类型
 */
@Getter
@AllArgsConstructor
public enum WorkOrderType {

    INSTALL_APPOINTMENT("安装预约"),
    REPAIR("维修"),
    REMOVE("退机"),
    RELOCATE("移机"),
    FILTER_REPLACE("滤芯更换");

    private final String description;
}
