package com.platform.workorder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单派单 DTO
 */
@Data
public class WorkOrderDispatchDTO implements Serializable {

    /**
     * 运维人员ID
     */
    @NotNull(message = "运维人员ID不能为空")
    private Long workerId;
}
