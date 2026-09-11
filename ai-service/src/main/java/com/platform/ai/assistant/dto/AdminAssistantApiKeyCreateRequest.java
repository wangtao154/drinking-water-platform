package com.platform.ai.assistant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** Request for a read-only external assistant credential. */
@Data
public class AdminAssistantApiKeyCreateRequest {

    @NotBlank(message = "请填写密钥名称")
    @Size(max = 64, message = "密钥名称不能超过64个字符")
    private String name;

    @Min(value = 1, message = "每分钟限流不能小于1")
    @Max(value = 120, message = "每分钟限流不能超过120")
    private Integer rateLimitPerMinute;

    /** Optional. A null value means the key does not expire. */
    private LocalDateTime expiresAt;
}
