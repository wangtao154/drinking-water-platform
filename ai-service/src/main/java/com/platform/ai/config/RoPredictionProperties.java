package com.platform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.ro")
public class RoPredictionProperties {

    private double defaultRatedPureLiters = 12000D;
    private int defaultRangeDays = 30;
    private String defaultAggregateEvery = "30m";
}
