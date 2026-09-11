package com.platform.ai.assistant.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminAssistantApiKeyVO {
    private Long id;
    private String name;
    private String keyPrefix;
    private String scope;
    private String status;
    private Integer rateLimitPerMinute;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private String lastUsedIpMasked;
    private LocalDateTime createdAt;
}
