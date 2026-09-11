package com.platform.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminAssistantApiKeyStatusRequest {
    @NotBlank(message = "请指定密钥状态")
    private String status;
}
