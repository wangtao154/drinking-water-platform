package com.platform.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RoMembranePredictionRequest {

    @NotBlank(message = "设备SN不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$", message = "设备SN格式不正确")
    private String sn;

    @Min(value = 1, message = "统计天数不能小于1")
    @Max(value = 180, message = "统计天数不能大于180")
    private Integer rangeDays;

    @Min(value = 100, message = "额定纯水量不能小于100L")
    private Double ratedPureLiters;

    @Pattern(regexp = "^[1-9][0-9]*(s|m|h)$", message = "聚合间隔格式不正确，例如10m、1h")
    private String aggregateEvery;
}
