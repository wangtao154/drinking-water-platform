package com.platform.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 客户创建工单 DTO
 */
@Data
public class WorkOrderCustomerCreateDTO {

    /**
     * 工单描述（文字内容）
     */
    @NotBlank(message = "工单描述不能为空")
    private String description;

    /**
     * 客户提交的图片URL列表（先上传再提交）
     */
    private List<String> customerImages;

    /**
     * 关联设备ID（可选）
     */
    private String deviceId;

    /**
     * 工单类型（客户可选：REPAIR/REMOVE/RELOCATE，默认 REPAIR）
     */
    private String orderType;
}
