package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新配置 DTO
 */
@Data
public class ConfigUpdateDTO {

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    /**
     * 配置描述
     */
    private String configDesc;
}
