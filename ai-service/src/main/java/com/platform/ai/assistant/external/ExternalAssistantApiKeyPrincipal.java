package com.platform.ai.assistant.external;

import com.platform.common.auth.CurrentUser;

/** Resolved external credential and its bound, permission-scoped backend account. */
public record ExternalAssistantApiKeyPrincipal(
        Long apiKeyId,
        String keyId,
        String keyName,
        Integer rateLimitPerMinute,
        CurrentUser user) {
}
