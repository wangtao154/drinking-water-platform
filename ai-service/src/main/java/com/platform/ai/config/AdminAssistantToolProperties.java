package com.platform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fixed, internal endpoints used by the read-only assistant data tools.
 * The model never receives these URLs and cannot select arbitrary targets.
 */
@Data
@ConfigurationProperties(prefix = "ai.assistant.tools")
public class AdminAssistantToolProperties {

    private boolean enabled = true;
    private int timeoutSeconds = 5;
    private String deviceBaseUrl = "http://device-service:8083";
    private String iotBaseUrl = "http://iot-service:8085";
    private String monitorBaseUrl = "http://monitor-service:8096";
    private String workOrderBaseUrl = "http://worker-order-service:8089";
    private String waterBaseUrl = "http://water-service:8100";
    private String reportBaseUrl = "http://report-service:8097";
    private String internalServiceToken;
}
