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

    private Long mockDefaultAmount = 1L;

    private Integer defaultDurationSeconds = 120;
}
