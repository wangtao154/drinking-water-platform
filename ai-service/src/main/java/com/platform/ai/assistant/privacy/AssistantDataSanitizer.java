package com.platform.ai.assistant.privacy;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single outbound-data boundary for the admin assistant.
 *
 * <p>Everything sent to a third-party model and every value retained in the
 * assistant audit trail goes through this component. Business tools still
 * expose their permitted summaries to the signed-in administrator, but the
 * model never receives raw personal, credential, payment or access data.</p>
 */
@Component
public class AssistantDataSanitizer {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<![0-9Xx])\\d{17}[0-9Xx](?![0-9Xx])");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern BANK_OR_TRANSACTION_PATTERN = Pattern.compile("(?<!\\d)\\d{16,32}(?!\\d)");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer\\s+[A-Z0-9._~-]{8,}");
    private static final Pattern STANDALONE_API_KEY_PATTERN = Pattern.compile("(?i)\\bsk-[A-Z0-9._~-]{8,}");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)https?://[^\\s,，;；]+");
    private static final Pattern SECRET_VALUE_PATTERN = Pattern.compile(
            "(?i)(api[-_ ]?key|appsecret|secret|password|passwd|token|access[_-]?token|refresh[_-]?token|private[_-]?key|merchant[_-]?key)\\s*([:=：])\\s*[^\\s,，;；]{4,}");
    private static final Pattern OPEN_ID_PATTERN = Pattern.compile(
            "(?i)(open[_-]?id|union[_-]?id)\\s*([:=：])\\s*[A-Z0-9_-]{8,}");
    private static final Pattern ADDRESS_VALUE_PATTERN = Pattern.compile(
            "(?i)(详细地址|收货地址|住址|地址)\\s*([:=：])\\s*[^\\n,，;；]{3,}");

    /** Sanitizes a prompt, tool summary or knowledge excerpt before model egress. */
    public String sanitizeForExternalModel(String value) {
        return sanitize(value);
    }

    /** Sanitizes values before audit summaries and digests are produced. */
    public String sanitizeForAudit(String value) {
        return sanitize(value);
    }

    /** Sanitizes generated text before it is shown or written to audit storage. */
    public String sanitizeModelOutput(String value) {
        return sanitize(value);
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String sanitized = value;
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [已脱敏]");
        sanitized = STANDALONE_API_KEY_PATTERN.matcher(sanitized).replaceAll("[已脱敏密钥]");
        sanitized = SECRET_VALUE_PATTERN.matcher(sanitized).replaceAll("$1$2[已脱敏]");
        sanitized = OPEN_ID_PATTERN.matcher(sanitized).replaceAll("$1$2[已脱敏]");
        sanitized = ADDRESS_VALUE_PATTERN.matcher(sanitized).replaceAll("$1$2[已脱敏]");
        sanitized = URL_PATTERN.matcher(sanitized).replaceAll("[已脱敏地址]");
        sanitized = maskMatches(PHONE_PATTERN, sanitized, match -> match.substring(0, 3) + "****" + match.substring(7));
        sanitized = maskMatches(ID_CARD_PATTERN, sanitized,
                match -> match.substring(0, 6) + "********" + match.substring(match.length() - 4));
        sanitized = maskMatches(EMAIL_PATTERN, sanitized, this::maskEmail);
        return BANK_OR_TRANSACTION_PATTERN.matcher(sanitized).replaceAll("[已脱敏编号]");
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        return (at <= 1 ? "***" : email.substring(0, 1) + "***") + email.substring(at);
    }

    private static String maskMatches(Pattern pattern, String source, java.util.function.Function<String, String> mapper) {
        Matcher matcher = pattern.matcher(source);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(mapper.apply(matcher.group())));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
