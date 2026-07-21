package com.platform.workorder.enums;

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
    IN_PROGRESS("进行中"),
    COMPLETED("待核验"),
    VERIFIED("已完成"),
    CANCELLED("已取消");

    private final String description;
}
