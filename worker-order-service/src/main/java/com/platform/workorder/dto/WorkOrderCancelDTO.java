package com.platform.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 取消工单 DTO
 */
@Data
public class WorkOrderCancelDTO implements Serializable {

    /**
     * 取消原因
     */
    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
