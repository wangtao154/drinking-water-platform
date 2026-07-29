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
     * Price in cents per liter. Set to 0 to charge the minimum amount only.
     */
    private Long unitPriceCentsPerLiter = 0L;

    private Long minTargetMl = 100L;

    private Long maxTargetMl = 10000L;

    private Integer defaultDurationSeconds = 120;
}
