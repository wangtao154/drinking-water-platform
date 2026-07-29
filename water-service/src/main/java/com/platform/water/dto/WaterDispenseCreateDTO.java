package com.platform.water.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WaterDispenseCreateDTO {

    private String deviceId;

    private String sn;

    @NotNull(message = "目标出水量不能为空")
    @Min(value = 1, message = "目标出水量必须大于0")
    private Long targetMl;

    /**
     * Deprecated: scan-water amount is calculated by the backend.
     */
    @Min(value = 1, message = "支付金额必须大于0")
    private Long payAmount;

    private String remark;
}
