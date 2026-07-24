package com.platform.water.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Q74PreviewDTO {

    @NotBlank(message = "动作不能为空")
    private String action;

    @NotNull(message = "目标出水量不能为空")
    @Min(value = 0, message = "目标出水量不能小于0")
    private Long targetMl;
}
