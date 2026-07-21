package com.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单状态
 */
@Getter
@AllArgsConstructor
public enum WorkOrderStatus {
    PENDING("待处理"),
    ASSIGNED("已派单"),
    ACCEPTED("已接单"),
    IN_PROGRESS("处理中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;
}
