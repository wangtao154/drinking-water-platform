package com.platform.ai.assistant.privacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantDataSanitizerTest {

    private final AssistantDataSanitizer sanitizer = new AssistantDataSanitizer();

    @Test
    void shouldMaskSensitiveValuesBeforeThirdPartyModelEgress() {
        String source = "手机号:13800138000，身份证:520102199001011234，邮箱:demo@example.com，"
                + "地址:贵州省遵义市测试路1号，openId:oiA1234567890，"
                + "token=sk-abcdefghijklmnopqrstuvwxyz，独立密钥 sk-zyxwvutsrqponmlkj，"
                + "接口地址:https://10.1.2.3/internal/api，交易号:45000003282026081139";

        String sanitized = sanitizer.sanitizeForExternalModel(source);

        assertTrue(sanitized.contains("138****8000"));
        assertTrue(sanitized.contains("520102********1234"));
        assertTrue(sanitized.contains("d***@example.com"));
        assertTrue(sanitized.contains("地址:[已脱敏]"));
        assertTrue(sanitized.contains("openId:[已脱敏]"));
        assertTrue(sanitized.contains("token=[已脱敏]"));
        assertTrue(sanitized.contains("[已脱敏密钥]"));
        assertTrue(sanitized.contains("接口地址:[已脱敏地址]"));
        assertTrue(sanitized.contains("[已脱敏编号]"));
        assertFalse(sanitized.contains("贵州省遵义市测试路1号"));
        assertFalse(sanitized.contains("sk-abcdefghijklmnopqrstuvwxyz"));
        assertFalse(sanitized.contains("sk-zyxwvutsrqponmlkj"));
        assertFalse(sanitized.contains("https://10.1.2.3/internal/api"));
        assertFalse(sanitized.contains("45000003282026081139"));
    }
}
