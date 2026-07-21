package com.platform.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundCreateDTO {

    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @NotNull(message = "退款金额不能为空")
    @Min(value = 1, message = "退款金额必须大于0")
    private Long refundAmount;

    private String refundReason;
}
