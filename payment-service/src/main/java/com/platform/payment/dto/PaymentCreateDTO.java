package com.platform.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCreateDTO {

    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @NotBlank(message = "支付方式不能为空")
    private String payMethod;

    @NotNull(message = "支付金额不能为空")
    @Min(value = 1, message = "支付金额必须大于0")
    private Long payAmount;
}
