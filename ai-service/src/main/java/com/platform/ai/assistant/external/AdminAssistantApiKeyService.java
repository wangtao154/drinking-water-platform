package com.platform.ai.assistant.external;

import com.platform.ai.assistant.dto.AdminAssistantApiKeyCreateRequest;
import com.platform.ai.assistant.dto.AdminAssistantApiKeyCreateResponse;
import com.platform.ai.assistant.dto.AdminAssistantApiKeyVO;
import com.platform.ai.config.AdminAssistantProperties;
import com.platform.common.auth.CurrentUser;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Creates, validates and revokes read-only keys for the external assistant API. */
@Service
@RequiredArgsConstructor
public class AdminAssistantApiKeyService {

    public static final String SCOPE_READ_ONLY_CHAT = "AI_CHAT_READONLY";
    private static final String KEY_PREFIX = "dwai_v1";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final AdminAssistantProperties properties;

    public AdminAssistantApiKeyCreateResponse create(AdminAssistantApiKeyCreateRequest request, CurrentUser creator) {
        if (creator == null || creator.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!creator.hasPermission("AI_ASSISTANT_VIEW")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账户没有 AI 助手使用权限，不能创建外部 API Key");
        }
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "失效时间必须晚于当前时间");
        }
        requireEligibleOwner(creator.getUserId());
        String keyId = randomHex(12);
        String secret = randomUrlToken(32);
        String apiKey = KEY_PREFIX + "_" + keyId + "_" + secret;
        String keyPrefix = KEY_PREFIX + "_" + keyId.substring(0, 8);
        int rateLimit = request.getRateLimitPerMinute() == null
                ? properties.getExternalApiDefaultRateLimitPerMinute()
                : request.getRateLimitPerMinute();
        jdbcTemplate.update("""
                        INSERT INTO ai_assistant_api_key
                        (key_id, key_prefix, key_hash, name, owner_id, scope, status, rate_limit_per_minute, expires_at, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, 'ENABLED', ?, ?, ?)
                        """,
                keyId, keyPrefix, sha256(secret), request.getName().trim(), creator.getUserId(), SCOPE_READ_ONLY_CHAT,
                rateLimit, request.getExpiresAt(), creator.getUserId());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return AdminAssistantApiKeyCreateResponse.builder()
                .id(id).name(request.getName().trim()).apiKey(apiKey).keyPrefix(keyPrefix)
                .scope(SCOPE_READ_ONLY_CHAT).rateLimitPerMinute(rateLimit).expiresAt(request.getExpiresAt())
                .warning("请立即保存 API Key。为保护凭据安全，系统不会再次显示完整密钥；该密钥仅可调用只读 AI 对话接口。")
                .build();
    }

    public List<AdminAssistantApiKeyVO> list(CurrentUser user) {
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return jdbcTemplate.query("""
                        SELECT id, name, key_prefix, scope, status, rate_limit_per_minute, expires_at,
                               last_used_at, last_used_ip, created_at
                        FROM ai_assistant_api_key
                        WHERE owner_id = ?
                        ORDER BY id DESC
                        """, keyRowMapper(), user.getUserId());
    }

    public void changeStatus(Long id, String requestedStatus, CurrentUser user) {
        if (id == null || user == null || user.getUserId() == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "密钥信息无效");
        }
        String status = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ENABLED", "DISABLED", "REVOKED").contains(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "密钥状态仅支持 ENABLED、DISABLED 或 REVOKED");
        }
        int updated = jdbcTemplate.update("""
                        UPDATE ai_assistant_api_key
                        SET status = ?, revoked_at = CASE WHEN ? = 'REVOKED' THEN NOW(3) ELSE revoked_at END
                        WHERE id = ? AND owner_id = ?
                        """, status, status, id, user.getUserId());
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到可管理的 API Key");
        }
    }

    public ExternalAssistantApiKeyPrincipal authenticate(String apiKey, String clientIp) {
        KeyParts parts = parseApiKey(apiKey);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                        SELECT k.id, k.key_id, k.name, k.key_hash, k.rate_limit_per_minute,
                               u.id AS owner_id, u.username, u.name AS owner_name, r.role_code
                        FROM ai_assistant_api_key k
                        JOIN sys_user u ON u.id = k.owner_id
                        JOIN sys_role r ON r.id = u.role_id
                        WHERE k.key_id = ?
                          AND k.status = 'ENABLED'
                          AND (k.expires_at IS NULL OR k.expires_at > NOW(3))
                          AND u.deleted = 0 AND u.status = 'ENABLED'
                          AND u.identity_verified = 1
                          AND u.name IS NOT NULL AND TRIM(u.name) <> ''
                          AND u.phone REGEXP '^1[3-9][0-9]{9}$'
                        """, parts.keyId());
        if (rows.size() != 1 || !MessageDigest.isEqual(
                String.valueOf(rows.get(0).get("key_hash")).getBytes(StandardCharsets.UTF_8),
                sha256(parts.secret()).getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "API Key 无效、已失效或已停用");
        }
        Map<String, Object> row = rows.get(0);
        Long ownerId = ((Number) row.get("owner_id")).longValue();
        Set<String> permissions = loadPermissions(ownerId);
        if (!permissions.contains("*") && !permissions.contains("AI_ASSISTANT_VIEW")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该 API Key 绑定账户已不具备 AI 助手使用权限");
        }
        CurrentUser owner = CurrentUser.builder()
                .userId(ownerId)
                .userName(StringUtils.hasText((String) row.get("owner_name")) ? (String) row.get("owner_name") : (String) row.get("username"))
                .userType((String) row.get("role_code"))
                .permissions(permissions)
                .build();
        Long keyPk = ((Number) row.get("id")).longValue();
        jdbcTemplate.update("UPDATE ai_assistant_api_key SET last_used_at = NOW(3), last_used_ip = ? WHERE id = ?",
                maskIp(clientIp), keyPk);
        return new ExternalAssistantApiKeyPrincipal(keyPk, (String) row.get("key_id"), (String) row.get("name"),
                ((Number) row.get("rate_limit_per_minute")).intValue(), owner);
    }

    public void recordAccess(ExternalAssistantApiKeyPrincipal principal, String requestId, String resultStatus,
                             Integer responseCode, long elapsedMs, String clientIp) {
        if (principal == null) {
            return;
        }
        jdbcTemplate.update("""
                        INSERT INTO ai_assistant_api_access_log
                        (api_key_id, request_id, endpoint, result_status, response_code, elapsed_ms, client_ip_masked, retention_until)
                        VALUES (?, ?, '/api/v1/ai/open/chat', ?, ?, ?, ?, DATE_ADD(NOW(3), INTERVAL 6 MONTH))
                        """, principal.apiKeyId(), requestId, resultStatus, responseCode, elapsedMs, maskIp(clientIp));
    }

    /** Stores rejected anonymous attempts without retaining an API secret. */
    public void recordRejectedAccess(String submittedKey, Integer responseCode, long elapsedMs, String clientIp) {
        jdbcTemplate.update("""
                        INSERT INTO ai_assistant_api_access_log
                        (api_key_id, key_hint, endpoint, result_status, response_code, elapsed_ms, client_ip_masked, retention_until)
                        VALUES (NULL, ?, '/api/v1/ai/open/chat', 'REJECTED', ?, ?, ?, DATE_ADD(NOW(3), INTERVAL 6 MONTH))
                        """, safeKeyHint(submittedKey), responseCode, elapsedMs, maskIp(clientIp));
    }

    private void requireEligibleOwner(Long ownerId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM sys_user
                        WHERE id = ? AND deleted = 0 AND status = 'ENABLED' AND identity_verified = 1
                          AND name IS NOT NULL AND TRIM(name) <> ''
                          AND phone REGEXP '^1[3-9][0-9]{9}$'
                        """, Integer.class, ownerId);
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账户未完成身份核验，不能创建外部 AI API Key");
        }
    }

    private Set<String> loadPermissions(Long ownerId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                        SELECT p.permission_code
                        FROM sys_user u
                        JOIN sys_role_permission rp ON rp.role_id = u.role_id AND (rp.deleted = 0 OR rp.deleted IS NULL)
                        JOIN sys_permission p ON p.id = rp.permission_id
                        WHERE u.id = ? AND p.status = 'ENABLED' AND (p.deleted = 0 OR p.deleted IS NULL)
                        """, String.class, ownerId));
    }

    private RowMapper<AdminAssistantApiKeyVO> keyRowMapper() {
        return (rs, ignored) -> AdminAssistantApiKeyVO.builder()
                .id(rs.getLong("id")).name(rs.getString("name")).keyPrefix(rs.getString("key_prefix"))
                .scope(rs.getString("scope")).status(rs.getString("status"))
                .rateLimitPerMinute(rs.getInt("rate_limit_per_minute"))
                .expiresAt(rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime())
                .lastUsedAt(rs.getTimestamp("last_used_at") == null ? null : rs.getTimestamp("last_used_at").toLocalDateTime())
                .lastUsedIpMasked(rs.getString("last_used_ip"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    private static KeyParts parseApiKey(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "缺少 X-API-Key");
        }
        String[] pieces = value.trim().split("_", 4);
        if (pieces.length != 4 || !"dwai".equals(pieces[0]) || !"v1".equals(pieces[1])
                || !pieces[2].matches("[0-9a-f]{24}") || pieces[3].length() < 32) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "API Key 格式无效");
        }
        return new KeyParts(pieces[2], pieces[3]);
    }

    private static String randomHex(int bytes) {
        byte[] random = new byte[bytes];
        SECURE_RANDOM.nextBytes(random);
        return HexFormat.of().formatHex(random);
    }

    private static String randomUrlToken(int bytes) {
        byte[] random = new byte[bytes];
        SECURE_RANDOM.nextBytes(random);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String maskIp(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String ip = value.trim();
        if (ip.contains(":")) {
            int boundary = ip.lastIndexOf(':');
            return boundary > 0 ? ip.substring(0, boundary) + ":*" : "*";
        }
        String[] parts = ip.split("\\.");
        return parts.length == 4 ? parts[0] + "." + parts[1] + "." + parts[2] + ".*" : "*";
    }

    private static String safeKeyHint(String value) {
        if (!StringUtils.hasText(value)) {
            return "missing";
        }
        String[] pieces = value.trim().split("_", 4);
        if (pieces.length >= 3 && "dwai".equals(pieces[0]) && "v1".equals(pieces[1])
                && pieces[2].matches("[0-9a-f]{24}")) {
            return KEY_PREFIX + "_" + pieces[2].substring(0, 8);
        }
        return "malformed";
    }

    private record KeyParts(String keyId, String secret) {
    }
}
