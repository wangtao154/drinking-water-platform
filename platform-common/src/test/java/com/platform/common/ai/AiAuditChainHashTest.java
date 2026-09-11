package com.platform.common.ai;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAuditChainHashTest {

    @Test
    void usesDatabaseMillisecondPrecisionForCreationTime() {
        LocalDateTime storedTime = LocalDateTime.of(2026, 9, 11, 9, 30, 0, 123_000_000);
        LocalDateTime runtimeTime = LocalDateTime.of(2026, 9, 11, 9, 30, 0, 123_987_654);

        assertEquals(
                AiAuditChainHash.calculate(null, payload(storedTime)),
                AiAuditChainHash.calculate(null, payload(runtimeTime)));
    }

    private static AiAuditChainHash.Payload payload(LocalDateTime createdAt) {
        return new AiAuditChainHash.Payload(
                "request-1", 1L, "超***", "qwen3.7-plus", "SUCCESS", false,
                null, null, "question-digest", "问题摘要", "answer-digest", "回答摘要",
                null, null, createdAt);
    }
}
