package com.platform.water.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WaterDispenseOrderPageQueryDTO extends PageQueryDTO {

    private String keyword;

    private String orderNo;

    private String sn;

    private String payStatus;

    private String dispenseStatus;

    private String commandStatus;
}
