package com.platform.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 手动创建工单 DTO
 */
@Data
public class WorkOrderCreateDTO implements Serializable {

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 客户ID
     */
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    /**
     * 经销商ID
     */
    @NotNull(message = "经销商ID不能为空")
    private Long dealerId;

    /**
     * 工单类型
     */
    @NotBlank(message = "工单类型不能为空")
    private String orderType;

    /**
     * 工单描述
     */
    private String description;

    /**
     * 预约时间
     */
    private LocalDateTime appointTime;

    /**
     * 优先级（1-4，默认3）
     */
    private Integer priority = 3;

    /**
     * 触发方式（默认 MANUAL）
     */
    private String triggerType = "MANUAL";
}
