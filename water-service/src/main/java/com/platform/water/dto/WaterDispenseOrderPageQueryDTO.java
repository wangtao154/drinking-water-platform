package com.platform.water.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class WaterDispenseOrderPageQueryDTO extends PageQueryDTO {

    private String keyword;

    private String orderNo;

    private String sn;

    private String payStatus;

    private String dispenseStatus;

    private String commandStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paidStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paidEndTime;
}
