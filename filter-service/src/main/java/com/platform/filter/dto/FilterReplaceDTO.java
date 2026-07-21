package com.platform.filter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 滤芯更换 DTO（C 端运维人员使用）
 */
@Data
public class FilterReplaceDTO {

    /**
     * 设备业务ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 旧滤芯编号（可为空，表示设备无旧滤芯直接安装新滤芯）
     */
    private String oldFilterId;

    /**
     * 新滤芯编号
     */
    @NotBlank(message = "新滤芯编号不能为空")
    private String newFilterId;

    /**
     * 可选：换芯备注
     */
    private String remark;
}
