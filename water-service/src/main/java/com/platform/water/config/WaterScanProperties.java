package com.platform.water.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "water.scan")
public class WaterScanProperties {

    private String protocolVersion = "SW1";

    private String commandPoint = "Q74";

    private String signSecret = "water-scan-sign-secret-change-me";

    /**
     * Minimum amount in cents. Kept at 1 cent for current WeChat Pay testing.
     */
    private Long minPayAmount = 1L;

    /**
     * Deprecated fallback price in cents per liter.
     */
    private Long unitPriceCentsPerLiter = 0L;

    /**
     * Cold water price in cents per liter.
     */
    private Long coldUnitPriceCentsPerLiter = 50L;

    /**
     * Hot water price in cents per liter.
     */
    private Long hotUnitPriceCentsPerLiter = 80L;

    private Long minTargetMl = 100L;

    private Long maxTargetMl = 100000L;

    private Integer defaultDurationSeconds = 120;

    private Integer commandMaxAttempts = 3;

    private Long commandAckTimeoutMs = 6000L;
}
