package com.platform.payment.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentPageQueryDTO extends PageQueryDTO {

    private String orderNo;
    private String payStatus;
    private String payMethod;
}
