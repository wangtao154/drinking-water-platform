package com.platform.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Explicit acknowledgement of the current backend AI assistant policy.
 */
@Data
public class AiAssistantConsentAcceptDTO {

    @NotBlank(message = "协议版本不能为空")
    private String policyVersion;

    @NotBlank(message = "同意来源不能为空")
    @Pattern(regexp = "LOGIN|ASSISTANT_PANEL", message = "不支持的同意来源")
    private String source;
}
