package com.platform.ai.assistant.audit;

import com.platform.ai.config.AdminAssistantAuditProperties;
import com.platform.ai.assistant.privacy.AssistantDataSanitizer;
import com.platform.common.ai.AiAuditChainHash;
import com.platform.common.auth.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Locale;

/** Writes minimal, searchable and privacy-protected AI compliance records. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAssistantAuditServiceImpl implements AdminAssistantAuditService {

    private final JdbcTemplate jdbcTemplate;
    private final AdminAssistantAuditProperties properties;
    private final AssistantDataSanitizer dataSanitizer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(AdminAssistantAuditEvent event) {
        if (event == null || event.user() == null || event.user().getUserId() == null) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            String question = dataSanitizer.sanitizeForAudit(event.question());
            String answer = dataSanitizer.sanitizeForAudit(event.answer());
            String errorMessage = dataSanitizer.sanitizeForAudit(event.errorMessage());
            String operatorNameMasked = maskUserName(event.user());
            LocalDateTime retentionUntil = now.plusMonths(properties.getRetentionMonths());
            String previousHash = lockAndReadLastHash();
            AiAuditChainHash.Payload payload = new AiAuditChainHash.Payload(
                    event.requestId(), event.user().getUserId(), operatorNameMasked, blankToNull(event.modelName()),
                    event.resultStatus(), event.fallback(), blankToNull(event.toolNames()),
                    blankToNull(event.knowledgeDocumentIds()), digest(question), summary(question),
                    digest(answer), summary(answer), blankToNull(event.errorCode()), summary(errorMessage), now);
            String recordHash = AiAuditChainHash.calculate(previousHash, payload);
            jdbcTemplate.update("""
                            INSERT INTO ai_assistant_audit_log
                            (request_id, operator_id, operator_name_masked, model_name, result_status, fallback,
                             tool_names, knowledge_document_ids, question_digest, question_summary_masked,
                             answer_digest, answer_summary_masked, error_code, error_summary_masked,
                             integrity_version, previous_hash, record_hash, retention_until, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    payload.requestId(), payload.operatorId(), payload.operatorNameMasked(), payload.modelName(),
                    payload.resultStatus(), payload.fallback(), payload.toolNames(), payload.knowledgeDocumentIds(),
                    payload.questionDigest(), payload.questionSummaryMasked(), payload.answerDigest(), payload.answerSummaryMasked(),
                    payload.errorCode(), payload.errorSummaryMasked(), AiAuditChainHash.VERSION, previousHash, recordHash,
                    retentionUntil, now);
            Long recordId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            jdbcTemplate.update("""
                            UPDATE ai_assistant_audit_chain_state
                            SET last_log_id = ?, last_record_hash = ?, updated_at = ?
                            WHERE id = 1
                            """, recordId, recordHash, now);
        } catch (Exception ex) {
            // The assistant remains available when compliance storage is temporarily unavailable.
            log.error("[AssistantAudit] audit write failed, requestId={}, operatorId={}, errorType={}",
                    event.requestId(), event.user().getUserId(), ex.getClass().getSimpleName());
        }
    }

    private String lockAndReadLastHash() {
        jdbcTemplate.update("""
                        INSERT IGNORE INTO ai_assistant_audit_chain_state (id, last_log_id, last_record_hash)
                        VALUES (1, NULL, NULL)
                        """);
        return jdbcTemplate.queryForObject("""
                        SELECT last_record_hash
                        FROM ai_assistant_audit_chain_state
                        WHERE id = 1
                        FOR UPDATE
                        """, (rs, rowNum) -> rs.getString("last_record_hash"));
    }

    @Override
    public void retainAfterAccountCancellation(Long accountId) {
        if (accountId == null) {
            return;
        }
        LocalDateTime cancelledAt = LocalDateTime.now();
        LocalDateTime minimumRetention = cancelledAt.plusMonths(properties.getRetentionMonths());
        jdbcTemplate.update("""
                        UPDATE ai_assistant_audit_log
                        SET account_cancelled_at = ?, retention_until = CASE
                            WHEN retention_until < ? THEN ?
                            ELSE retention_until END
                        WHERE operator_id = ?
                        """, cancelledAt, minimumRetention, minimumRetention, accountId);
        log.info("[AssistantAudit] account retention hold applied, accountId={}, retainUntil={}",
                accountId, minimumRetention);
    }

    @Override
    @Scheduled(cron = "${ai.assistant.audit.cleanup-cron:0 15 3 * * *}")
    public int purgeExpired() {
        int deleted = jdbcTemplate.update("DELETE FROM ai_assistant_audit_log WHERE retention_until < NOW(3) LIMIT 1000");
        if (deleted > 0) {
            log.info("[AssistantAudit] expired audit records purged: {}", deleted);
        }
        return deleted;
    }

    private static String digest(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String summary(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 237) + "...";
    }

    private static String maskUserName(CurrentUser user) {
        String name = user.getUserName();
        if (!StringUtils.hasText(name)) {
            return "账户" + user.getUserId();
        }
        return name.length() <= 1 ? "*" : name.substring(0, 1) + "***";
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
