package com.platform.ai.assistant.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** The secret is returned once when a key is created and is never stored in plaintext. */
@Data
@Builder
public class AdminAssistantApiKeyCreateResponse {
    private Long id;
    private String name;
    private String apiKey;
    private String keyPrefix;
    private String scope;
    private Integer rateLimitPerMinute;
    private LocalDateTime expiresAt;
    private String warning;
}
