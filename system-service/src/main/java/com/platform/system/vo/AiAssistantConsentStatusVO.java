package com.platform.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Current user's acknowledgement status for the backend AI assistant policy.
 */
@Data
public class AiAssistantConsentStatusVO {

    private String policyVersion;
    private String policyTitle;
    private Boolean accepted;
    private LocalDateTime acceptedAt;
    private LocalDateTime lastConfirmedAt;
}
