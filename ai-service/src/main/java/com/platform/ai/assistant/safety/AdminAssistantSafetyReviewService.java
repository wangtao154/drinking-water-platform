package com.platform.ai.assistant.safety;

import com.platform.ai.assistant.privacy.AssistantDataSanitizer;
import com.platform.ai.config.AdminAssistantAuditProperties;
import com.platform.common.auth.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Writes a masked safety-review task into the existing local complaint workflow. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAssistantSafetyReviewService {

    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final JdbcTemplate jdbcTemplate;
    private final AssistantDataSanitizer dataSanitizer;
    private final AdminAssistantAuditProperties auditProperties;

    public Optional<String> createReview(CurrentUser user, String requestId,
                                         AdminAssistantContentSafetyService.SafetyDecision decision,
                                         String question) {
        if (user == null || user.getUserId() == null || decision == null || !decision.blocked()) {
            return Optional.empty();
        }
        try {
            String reviewNo = "AISAFE" + LocalDateTime.now().format(NUMBER_TIME)
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            String maskedQuestion = dataSanitizer.sanitizeForAudit(question);
            String content = "系统内容安全自动拦截；规则=" + decision.ruleCode() + "（" + decision.ruleLabel()
                    + "）；脱敏摘要=" + abbreviate(maskedQuestion, 700);
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update("""
                            INSERT INTO ai_assistant_complaint
                                (complaint_no, reporter_id, reporter_name, category, content, request_id,
                                 status, reply_due_at, retention_until, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, 'PENDING', DATE_ADD(NOW(3), INTERVAL 3 DAY), ?, ?)
                            """,
                    reviewNo, user.getUserId(), maskUserName(user), decision.complaintCategory(), content, requestId,
                    now.plusMonths(auditProperties.getRetentionMonths()), now);
            log.warn("[AssistantSafety] review created, reviewNo={}, requestId={}, operatorId={}, rule={}",
                    reviewNo, requestId, user.getUserId(), decision.ruleCode());
            return Optional.of(reviewNo);
        } catch (Exception ex) {
            log.error("[AssistantSafety] review write failed, requestId={}, operatorId={}, rule={}, errorType={}",
                    requestId, user.getUserId(), decision.ruleCode(), ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static String maskUserName(CurrentUser user) {
        String name = user.getUserName();
        if (!StringUtils.hasText(name)) {
            return "账户" + user.getUserId();
        }
        return name.length() <= 1 ? "*" : name.substring(0, 1) + "***";
    }

    private static String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "未提供";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
