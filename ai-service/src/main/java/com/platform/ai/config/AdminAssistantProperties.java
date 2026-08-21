package com.platform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the controlled, read-only admin assistant.
 */
@Data
@ConfigurationProperties(prefix = "ai.assistant")
public class AdminAssistantProperties {

    private boolean enabled = true;
    private int maxQuestionChars = 1000;
    private int maxKnowledgeDocuments = 4;
    private int maxKnowledgeChars = 18000;
    private int maxAnswerChars = 4000;
    private int rateLimitWindowSeconds = 60;
    private int maxRequestsPerUser = 8;
}
