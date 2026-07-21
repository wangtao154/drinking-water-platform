package com.platform.finance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvoiceCreateDTO {

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotNull(message = "发票金额不能为空")
    @Min(value = 1, message = "发票金额必须大于0")
    private Long amount;

    @NotBlank(message = "发票抬头不能为空")
    private String title;

    private String taxNo;

    private String mailingAddress;
}
