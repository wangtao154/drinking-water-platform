package com.platform.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricStatsVO {

    private String field;
    private String name;
    private String unit;
    private Integer count;
    private Double first;
    private Double last;
    private Double min;
    private Double max;
    private Double avg;
    private Double delta;
}
