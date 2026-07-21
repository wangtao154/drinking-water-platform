package com.platform.finance.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SettlementPageQueryDTO extends PageQueryDTO {

    private Long dealerId;
    private String status;
}
