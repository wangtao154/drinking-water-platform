package com.platform.workorder.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统自动创建滤芯更换工单 DTO
 */
@Data
public class WorkOrderFilterReplaceDTO implements Serializable {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 旧滤芯ID
     */
    private String oldFilterId;

    /**
     * 新滤芯ID
     */
    private String newFilterId;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 经销商ID
     */
    private Long dealerId;
}
