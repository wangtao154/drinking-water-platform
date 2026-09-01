package com.platform.common.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Hashes the immutable portion of an AI audit record into a verifiable chain.
 * Account cancellation and retention fields are intentionally excluded because
 * compliance workflows are allowed to extend them after the record is created.
 */
public final class AiAuditChainHash {

    public static final String VERSION = "AI_AUDIT_CHAIN_V1";

    private AiAuditChainHash() {
    }

    public static String calculate(String previousHash, Payload payload) {
        String canonical = String.join("|",
                VERSION,
                nullable(previousHash),
                nullable(payload.requestId()),
                nullable(payload.operatorId()),
                nullable(payload.operatorNameMasked()),
                nullable(payload.modelName()),
                nullable(payload.resultStatus()),
                Boolean.toString(payload.fallback()),
                nullable(payload.toolNames()),
                nullable(payload.knowledgeDocumentIds()),
                nullable(payload.questionDigest()),
                nullable(payload.questionSummaryMasked()),
                nullable(payload.answerDigest()),
                nullable(payload.answerSummaryMasked()),
                nullable(payload.errorCode()),
                nullable(payload.errorSummaryMasked()),
                nullable(payload.createdAt() == null ? null : payload.createdAt().toString()));
        return sha256(canonical);
    }

    public static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String nullable(Object value) {
        if (value == null) {
            return "-";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
    }

    public record Payload(
            String requestId,
            Long operatorId,
            String operatorNameMasked,
            String modelName,
            String resultStatus,
            boolean fallback,
            String toolNames,
            String knowledgeDocumentIds,
            String questionDigest,
            String questionSummaryMasked,
            String answerDigest,
            String answerSummaryMasked,
            String errorCode,
            String errorSummaryMasked,
            LocalDateTime createdAt) {
    }
}
