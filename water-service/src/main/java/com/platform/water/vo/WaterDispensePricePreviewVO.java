package com.platform.water.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WaterDispensePricePreviewVO {

    private Long targetMl;

    private Integer waterType;

    private Long payAmount;
}
