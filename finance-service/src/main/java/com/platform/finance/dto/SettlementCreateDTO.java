package com.platform.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SettlementCreateDTO {

    @NotNull(message = "经销商ID不能为空")
    private Long dealerId;

    @NotBlank(message = "账单开始日期不能为空")
    private String billStartDate;

    @NotBlank(message = "账单结束日期不能为空")
    private String billEndDate;
}
