package com.platform.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateDTO {

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    private String customerName;

    private String customerPhone;

    private String customerWechat;

    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    private String productName;

    @NotNull(message = "订单金额不能为空")
    @Min(value = 0, message = "订单金额不能为负")
    private Long orderAmount;

    private Long faceValue;

    @NotNull(message = "支付金额不能为空")
    @Min(value = 0, message = "支付金额不能为负")
    private Long payAmount;

    private String payMethod;

    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    private Long dealerId;

    private String dealerName;
}
