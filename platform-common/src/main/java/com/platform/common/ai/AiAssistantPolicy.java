package com.platform.common.ai;

/**
 * Backend AI assistant policy identifiers shared by all services.
 *
 * Keeping the version in platform-common prevents the system service and the
 * isolated AI service from accepting different agreement versions.
 */
public final class AiAssistantPolicy {

    public static final String CONSENT_TYPE = "PLATFORM_USER_PRIVACY";
    public static final String VERSION = "2026-08-26-v2";

    private AiAssistantPolicy() {
    }
}
