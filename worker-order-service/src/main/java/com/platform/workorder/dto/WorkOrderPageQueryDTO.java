package com.platform.workorder.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单分页查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderPageQueryDTO extends PageQueryDTO {

    /**
     * 工单状态
     */
    private String orderStatus;

    /**
     * 工单类型
     */
    private String orderType;

    /**
     * 运维人员ID
     */
    private Long workerId;

    /**
     * 经销商ID
     */
    private Long dealerId;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 触发方式
     */
    private String triggerType;

    /**
     * 关键词搜索（工单号模糊匹配）
     */
    private String keyword;
}
