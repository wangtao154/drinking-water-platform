package com.platform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Retention settings for minimal AI compliance audit records. */
@Data
@ConfigurationProperties(prefix = "ai.assistant.audit")
public class AdminAssistantAuditProperties {

    /** Minimum retention period, including after account cancellation. */
    private int retentionMonths = 6;
}
