package com.platform.filter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FilterModelCreateDTO {

    @NotBlank(message = "型号名称不能为空")
    @Size(max = 128, message = "型号名称最长128字符")
    private String modelName;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotNull(message = "滤芯级别不能为空")
    @Min(value = 1, message = "滤芯级别最小为1")
    private Integer filterLevel;

    @NotNull(message = "标准寿命时长不能为空")
    @Min(value = 1, message = "标准寿命时长最小为1")
    private Integer standardLifeDuration;

    @NotNull(message = "标准寿命流量不能为空")
    @Min(value = 0, message = "标准寿命流量最小为0")
    private Long standardLifeFlow;

    private Long price;

    @Size(max = 512, message = "描述最长512字符")
    private String description;
}
