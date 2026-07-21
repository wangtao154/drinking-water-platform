package com.platform.filter.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FilterRegisterBatchDTO {

    @NotEmpty(message = "批量登记列表不能为空")
    private List<FilterRegisterDTO> filters;
}
