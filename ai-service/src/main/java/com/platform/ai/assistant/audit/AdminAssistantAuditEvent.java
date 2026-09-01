package com.platform.ai.assistant.audit;

import com.platform.common.auth.CurrentUser;

/**
 * Compliance-safe audit payload. Raw questions and answers are deliberately
 * excluded from persistence; the writer derives only digests and masked summaries.
 */
public record AdminAssistantAuditEvent(
        String requestId,
        CurrentUser user,
        String modelName,
        String resultStatus,
        boolean fallback,
        String toolNames,
        String knowledgeDocumentIds,
        String question,
        String answer,
        String errorCode,
        String errorMessage) {
}
