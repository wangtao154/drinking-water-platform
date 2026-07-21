package com.platform.filter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FilterModelUpdateDTO {

    @Size(max = 128, message = "型号名称最长128字符")
    private String modelName;

    private String category;

    @Min(value = 1, message = "滤芯级别最小为1")
    private Integer filterLevel;

    @Min(value = 1, message = "标准寿命时长最小为1")
    private Integer standardLifeDuration;

    @Min(value = 0, message = "标准寿命流量最小为0")
    private Long standardLifeFlow;

    private Long price;

    @Size(max = 512, message = "描述最长512字符")
    private String description;

    private String status;
}
