package com.platform.workorder.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单流转日志 VO
 */
@Data
public class WorkOrderLogVO implements Serializable {

    private Long id;

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 变更前状态
     */
    private String fromStatus;

    /**
     * 变更后状态
     */
    private String toStatus;

    /**
     * 变更前状态描述
     */
    private String fromStatusDesc;

    /**
     * 变更后状态描述
     */
    private String toStatusDesc;

    /**
     * 操作人类型（ADMIN/DEALER/WORKER/CUSTOMER）
     */
    private String operatorType;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作时间
     */
    private LocalDateTime createdAt;
}
