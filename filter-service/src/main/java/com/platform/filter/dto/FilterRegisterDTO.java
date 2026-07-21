package com.platform.filter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FilterRegisterDTO {

    @NotNull(message = "滤芯型号ID不能为空")
    private Long filterModelId;

    @PastOrPresent(message = "生产日期不能是未来日期")
    private LocalDate productionDate;

    private String productionBatch;
}
