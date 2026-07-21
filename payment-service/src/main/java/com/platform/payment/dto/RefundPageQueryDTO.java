package com.platform.payment.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RefundPageQueryDTO extends PageQueryDTO {

    private String orderNo;
    private String refundStatus;
}
