package com.platform.order.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageQueryDTO extends PageQueryDTO {

    private String orderNo;

    private Long customerId;

    private String orderStatus;

    private String orderType;

    private Long dealerId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
