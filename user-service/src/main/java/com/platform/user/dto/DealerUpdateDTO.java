package com.platform.user.dto;

import lombok.Data;

@Data
public class DealerUpdateDTO {

    private String dealerName;

    private String contactPerson;

    private String contactPhone;

    private String address;

    /** 经销商等级（前端数字，如 1/2/3，映射到 L1_DEALER/L2_DEALER/L3_DEALER） */
    private Integer level;
}
