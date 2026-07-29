package com.platform.water.vo;

import lombok.Data;

@Data
public class WaterDispenseOrderStatsVO {

    private Long total;

    private Long paid;

    private Long pending;

    private Long dispatched;

    private Long sent;

    private Long totalAmount;
}
