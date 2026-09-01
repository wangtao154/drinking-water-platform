package com.platform.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.auth.UserContext;
import com.platform.common.ai.AiAssistantPolicy;
import com.platform.common.ai.AiAuditChainHash;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.system.dto.AuditLogPageQueryDTO;
import com.platform.system.dto.AiAssistantAuditLogPageQueryDTO;
import com.platform.system.dto.AiAssistantComplaintCreateDTO;
import com.platform.system.dto.AiAssistantComplaintHandleDTO;
import com.platform.system.dto.AiAssistantComplaintPageQueryDTO;
import com.platform.system.dto.AiAssistantConsentAcceptDTO;
import com.platform.system.dto.AccountCreateDTO;
import com.platform.system.dto.AccountIdentityVerificationDTO;
import com.platform.system.dto.AccountSelfCancellationDTO;
import com.platform.system.dto.AccountUpdateDTO;
import com.platform.system.dto.ConfigUpdateDTO;
import com.platform.system.dto.MqttConfigUpdateDTO;
import com.platform.system.dto.RoleCreateDTO;
import com.platform.system.dto.RolePermissionUpdateDTO;
import com.platform.system.entity.AuditLog;
import com.platform.system.entity.SysAccount;
import com.platform.system.entity.SysConfig;
import com.platform.system.entity.SysPermission;
import com.platform.system.entity.SysRole;
import com.platform.system.entity.SysRolePermission;
import com.platform.system.feign.IotServiceClient;
import com.platform.system.mapper.AuditLogMapper;
import com.platform.system.mapper.SysAccountMapper;
import com.platform.system.mapper.SysConfigMapper;
import com.platform.system.mapper.SysPermissionMapper;
import com.platform.system.mapper.SysRoleMapper;
import com.platform.system.mapper.SysRolePermissionMapper;
import com.platform.system.service.SystemService;
import com.platform.system.vo.AuditLogVO;
import com.platform.system.vo.AiAssistantAuditLogVO;
import com.platform.system.vo.AiAssistantAuditIntegrityVO;
import com.platform.system.vo.AiAssistantComplaintVO;
import com.platform.system.vo.AiAssistantConsentStatusVO;
import com.platform.system.vo.MqttConfigVO;
import com.platform.system.vo.SysAccountVO;
import com.platform.system.vo.SysConfigVO;
import com.platform.system.vo.SysPermissionVO;
import com.platform.system.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * 系统管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final String IDENTITY_VERIFICATION_METHOD = "MANUAL_NAME_PHONE_REVIEW";
    private static final String IDENTITY_INVALIDATED_METHOD = "DETAIL_CHANGED";
    private static final Set<String> AI_COMPLAINT_CATEGORIES = Set.of(
            "CONTENT_QUALITY", "DATA_ISSUE", "SECURITY_PRIVACY", "MISUSE_REPORT");
    private static final Set<String> AI_COMPLAINT_STATUSES = Set.of(
            "PENDING", "PROCESSING", "RESOLVED", "REJECTED");
    private static final String AI_ASSISTANT_POLICY_TITLE = "直饮水平台用户协议与隐私政策";
    private static final String ACCOUNT_CANCELLATION_CONFIRM_TEXT = "注销账户";
    private static final Set<String> BACKEND_ACCOUNT_USER_TYPES = Set.of(
            "SUPER_ADMIN", "ADMIN", "OPERATOR", "FINANCE");
    private static final List<String> TOKEN_USER_TYPES = List.of(
            "SUPER_ADMIN", "ADMIN", "OPERATOR", "FINANCE", "DEALER", "WORKER", "CUSTOMER", "GUEST");
    private static final String REDIS_ACCESS_TOKEN_KEY = "auth:token:access:";
    private static final String REDIS_REFRESH_TOKEN_KEY = "auth:token:refresh:";
    private static final String REDIS_SESSION_KEY = "auth:session:";

    private final SysConfigMapper sysConfigMapper;
    private final AuditLogMapper auditLogMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysAccountMapper sysAccountMapper;
    private final IotServiceClient iotServiceClient;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public List<SysConfigVO> listConfigs() {
        List<SysConfig> list = sysConfigMapper.selectList(null);
        return list.stream()
                .map(SysConfigVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public SysConfigVO getConfigByKey(String key) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig entity = sysConfigMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置项不存在: " + key);
        }
        return SysConfigVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfigVO updateConfig(Long id, ConfigUpdateDTO dto) {
        SysConfig entity = sysConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置项不存在");
        }
        entity.setConfigValue(dto.getConfigValue());
        if (dto.getConfigDesc() != null) {
            entity.setConfigDesc(dto.getConfigDesc());
        }
        sysConfigMapper.updateById(entity);
        log.info("[系统配置] 更新配置: id={}, key={}", id, entity.getConfigKey());
        return SysConfigVO.fromEntity(entity);
    }

    @Override
    public PageResult<AuditLogVO> auditLogPage(AuditLogPageQueryDTO query) {
        query.normalize();
        Page<AuditLog> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getOperatorId() != null) {
            wrapper.eq(AuditLog::getOperatorId, query.getOperatorId());
        }
        if (query.getOperation() != null && !query.getOperation().isEmpty()) {
            wrapper.eq(AuditLog::getOperation, query.getOperation());
        }
        if (query.getResult() != null && !query.getResult().isEmpty()) {
            wrapper.eq(AuditLog::getResult, query.getResult());
        }
        if (query.getStartTime() != null && !query.getStartTime().isEmpty()) {
            wrapper.ge(AuditLog::getCreatedAt, LocalDate.parse(query.getStartTime()).atStartOfDay());
        }
        if (query.getEndTime() != null && !query.getEndTime().isEmpty()) {
            wrapper.le(AuditLog::getCreatedAt, LocalDate.parse(query.getEndTime()).atTime(LocalTime.MAX));
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);

        Page<AuditLog> result = auditLogMapper.selectPage(page, wrapper);
        List<AuditLogVO> voList = result.getRecords().stream()
                .map(AuditLogVO::fromEntity)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getSize(),
                result.getCurrent(), voList);
    }

    @Override
    public PageResult<AiAssistantAuditLogVO> aiAssistantAuditLogPage(AiAssistantAuditLogPageQueryDTO query) {
        query.normalize();
        StringBuilder where = new StringBuilder(" FROM ai_assistant_audit_log WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (query.getOperatorId() != null) {
            where.append(" AND operator_id = ?");
            params.add(query.getOperatorId());
        }
        if (StringUtils.hasText(query.getModelName())) {
            where.append(" AND model_name = ?");
            params.add(query.getModelName().trim());
        }
        if (StringUtils.hasText(query.getResultStatus())) {
            where.append(" AND result_status = ?");
            params.add(query.getResultStatus().trim());
        }
        if (StringUtils.hasText(query.getStartTime())) {
            where.append(" AND created_at >= ?");
            params.add(LocalDate.parse(query.getStartTime()).atStartOfDay());
        }
        if (StringUtils.hasText(query.getEndTime())) {
            where.append(" AND created_at <= ?");
            params.add(LocalDate.parse(query.getEndTime()).atTime(LocalTime.MAX));
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(query.getPageSize());
        pageParams.add((query.getPageNum() - 1) * query.getPageSize());
        List<AiAssistantAuditLogVO> records = jdbcTemplate.query("""
                        SELECT id, request_id, operator_id, operator_name_masked, model_name, result_status, fallback,
                               tool_names, knowledge_document_ids, question_summary_masked, answer_summary_masked,
                               error_code, error_summary_masked, account_cancelled_at, retention_until, created_at
                        """ + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?", (rs, rowNum) -> {
                    AiAssistantAuditLogVO vo = new AiAssistantAuditLogVO();
                    vo.setId(rs.getLong("id"));
                    vo.setRequestId(rs.getString("request_id"));
                    vo.setOperatorId(rs.getLong("operator_id"));
                    vo.setOperatorNameMasked(rs.getString("operator_name_masked"));
                    vo.setModelName(rs.getString("model_name"));
                    vo.setResultStatus(rs.getString("result_status"));
                    vo.setFallback(rs.getBoolean("fallback"));
                    vo.setToolNames(rs.getString("tool_names"));
                    vo.setKnowledgeDocumentIds(rs.getString("knowledge_document_ids"));
                    vo.setQuestionSummaryMasked(rs.getString("question_summary_masked"));
                    vo.setAnswerSummaryMasked(rs.getString("answer_summary_masked"));
                    vo.setErrorCode(rs.getString("error_code"));
                    vo.setErrorSummaryMasked(rs.getString("error_summary_masked"));
                    var cancelledAt = rs.getTimestamp("account_cancelled_at");
                    var retentionUntil = rs.getTimestamp("retention_until");
                    var createdAt = rs.getTimestamp("created_at");
                    vo.setAccountCancelledAt(cancelledAt == null ? null : cancelledAt.toLocalDateTime());
                    vo.setRetentionUntil(retentionUntil == null ? null : retentionUntil.toLocalDateTime());
                    vo.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                    return vo;
                }, pageParams.toArray());
        return new PageResult<>(total == null ? 0L : total, query.getPageSize(),
                query.getPageNum().longValue(), records);
    }

    @Override
    public AiAssistantAuditIntegrityVO verifyAiAssistantAuditIntegrity() {
        List<AuditChainRow> rows = jdbcTemplate.query("""
                        SELECT id, request_id, operator_id, operator_name_masked, model_name, result_status, fallback,
                               tool_names, knowledge_document_ids, question_digest, question_summary_masked,
                               answer_digest, answer_summary_masked, error_code, error_summary_masked,
                               integrity_version, previous_hash, record_hash, created_at
                        FROM ai_assistant_audit_log
                        ORDER BY id ASC
                        """, (rs, rowNum) -> new AuditChainRow(
                rs.getLong("id"), rs.getString("request_id"), rs.getLong("operator_id"),
                rs.getString("operator_name_masked"), rs.getString("model_name"), rs.getString("result_status"),
                rs.getBoolean("fallback"), rs.getString("tool_names"), rs.getString("knowledge_document_ids"),
                rs.getString("question_digest"), rs.getString("question_summary_masked"),
                rs.getString("answer_digest"), rs.getString("answer_summary_masked"), rs.getString("error_code"),
                rs.getString("error_summary_masked"), rs.getString("integrity_version"),
                rs.getString("previous_hash"), rs.getString("record_hash"),
                rs.getTimestamp("created_at").toLocalDateTime()));

        AiAssistantAuditIntegrityVO result = new AiAssistantAuditIntegrityVO();
        result.setTotalRecords((long) rows.size());
        result.setVerifiedAt(LocalDateTime.now());

        long legacyUnsealed = 0;
        long checked = 0;
        String previousHash = null;
        AuditChainRow lastSealed = null;
        for (AuditChainRow row : rows) {
            if (!StringUtils.hasText(row.recordHash())) {
                if (lastSealed != null) {
                    return integrityFailure(result, legacyUnsealed, checked, row,
                            "发现未封存记录插入到哈希链中间，审计链不完整");
                }
                legacyUnsealed++;
                continue;
            }
            if (!AiAuditChainHash.VERSION.equals(row.integrityVersion())) {
                return integrityFailure(result, legacyUnsealed, checked, row, "发现不支持的审计完整性版本");
            }
            if (!Objects.equals(previousHash, row.previousHash())) {
                return integrityFailure(result, legacyUnsealed, checked, row, "上一条审计哈希不匹配");
            }
            String expectedHash = AiAuditChainHash.calculate(previousHash, row.toPayload());
            if (!Objects.equals(expectedHash, row.recordHash())) {
                return integrityFailure(result, legacyUnsealed, checked, row, "审计记录哈希校验不通过");
            }
            previousHash = row.recordHash();
            lastSealed = row;
            checked++;
        }

        AuditChainState chainState = jdbcTemplate.query("""
                        SELECT last_log_id, last_record_hash
                        FROM ai_assistant_audit_chain_state
                        WHERE id = 1
                        """, rs -> rs.next() ? new AuditChainState(
                rs.getObject("last_log_id", Long.class), rs.getString("last_record_hash")) : null);
        if (lastSealed != null && (chainState == null || !Objects.equals(chainState.lastLogId(), lastSealed.id())
                || !Objects.equals(chainState.lastRecordHash(), lastSealed.recordHash()))) {
            return integrityFailure(result, legacyUnsealed, checked, lastSealed,
                    "审计链状态与最后一条审计记录不一致");
        }

        result.setCheckedRecords(checked);
        result.setLegacyUnsealedRecords(legacyUnsealed);
        if (legacyUnsealed > 0) {
            result.setStatus("LEGACY_UNSEALED");
            result.setMessage("校验通过：存在 " + legacyUnsealed + " 条哈希链上线前的历史记录，后续新增记录已纳入完整性校验");
        } else {
            result.setStatus("VERIFIED");
            result.setMessage("完整性校验通过，未发现审计日志篡改或缺失迹象");
        }
        return result;
    }

    private AiAssistantAuditIntegrityVO integrityFailure(AiAssistantAuditIntegrityVO result,
                                                          long legacyUnsealed,
                                                          long checked,
                                                          AuditChainRow row,
                                                          String message) {
        result.setStatus("FAILED");
        result.setMessage(message);
        result.setCheckedRecords(checked);
        result.setLegacyUnsealedRecords(legacyUnsealed);
        result.setFirstProblemRecordId(row.id());
        result.setFirstProblemCreatedAt(row.createdAt());
        return result;
    }

    private record AuditChainRow(
            Long id, String requestId, Long operatorId, String operatorNameMasked, String modelName,
            String resultStatus, boolean fallback, String toolNames, String knowledgeDocumentIds,
            String questionDigest, String questionSummaryMasked, String answerDigest, String answerSummaryMasked,
            String errorCode, String errorSummaryMasked, String integrityVersion, String previousHash,
            String recordHash, LocalDateTime createdAt) {
        AiAuditChainHash.Payload toPayload() {
            return new AiAuditChainHash.Payload(requestId, operatorId, operatorNameMasked, modelName, resultStatus,
                    fallback, toolNames, knowledgeDocumentIds, questionDigest, questionSummaryMasked, answerDigest,
                    answerSummaryMasked, errorCode, errorSummaryMasked, createdAt);
        }
    }

    private record AuditChainState(Long lastLogId, String lastRecordHash) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAssistantComplaintVO createAiAssistantComplaint(AiAssistantComplaintCreateDTO dto) {
        String category = normalizeComplaintCategory(dto.getCategory());
        Long reporterId = UserContext.getUserId();
        String reporterName = UserContext.get() != null && StringUtils.hasText(UserContext.get().getUserName())
                ? UserContext.get().getUserName().trim() : "后台用户";
        String complaintNo = buildComplaintNo();
        jdbcTemplate.update("""
                        INSERT INTO ai_assistant_complaint
                            (complaint_no, reporter_id, reporter_name, category, content, request_id,
                             status, reply_due_at, retention_until)
                        VALUES (?, ?, ?, ?, ?, ?, 'PENDING', DATE_ADD(NOW(3), INTERVAL 3 DAY),
                                DATE_ADD(NOW(3), INTERVAL 6 MONTH))
                        """,
                complaintNo, reporterId, reporterName, category, dto.getContent().trim(),
                StringUtils.hasText(dto.getRequestId()) ? dto.getRequestId().trim() : null);
        log.info("[AI投诉举报] 已提交: complaintNo={}, reporterId={}, category={}",
                complaintNo, reporterId, category);
        return findAiAssistantComplaintByNo(complaintNo);
    }

    @Override
    public AiAssistantConsentStatusVO getAiAssistantConsentStatus() {
        Long accountId = requireCurrentAccountId();
        List<AiAssistantConsentStatusVO> rows = jdbcTemplate.query("""
                        SELECT accepted, accepted_at, last_confirmed_at
                        FROM ai_assistant_consent_log
                        WHERE account_id = ? AND consent_type = ? AND policy_version = ?
                        """, (rs, rowNum) -> {
                    AiAssistantConsentStatusVO vo = new AiAssistantConsentStatusVO();
                    vo.setAccepted(rs.getBoolean("accepted"));
                    vo.setAcceptedAt(toLocalDateTime(rs.getTimestamp("accepted_at")));
                    vo.setLastConfirmedAt(toLocalDateTime(rs.getTimestamp("last_confirmed_at")));
                    return vo;
                }, accountId, AiAssistantPolicy.CONSENT_TYPE, AiAssistantPolicy.VERSION);
        AiAssistantConsentStatusVO status = rows.isEmpty() ? new AiAssistantConsentStatusVO() : rows.get(0);
        status.setPolicyVersion(AiAssistantPolicy.VERSION);
        status.setPolicyTitle(AI_ASSISTANT_POLICY_TITLE);
        if (status.getAccepted() == null) {
            status.setAccepted(false);
        }
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAssistantConsentStatusVO acceptAiAssistantConsent(AiAssistantConsentAcceptDTO dto) {
        if (!AiAssistantPolicy.VERSION.equals(dto.getPolicyVersion().trim())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "协议版本已更新，请重新阅读后确认");
        }
        Long accountId = requireCurrentAccountId();
        SysAccount account = sysAccountMapper.selectById(accountId);
        if (account == null || !"ENABLED".equals(account.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "后台账户不存在或已停用");
        }
        String accountName = StringUtils.hasText(account.getName()) ? account.getName().trim() : account.getUsername();
        jdbcTemplate.update("""
                        INSERT INTO ai_assistant_consent_log
                            (account_id, account_name, consent_type, policy_version, accepted, consent_source,
                             accepted_at, last_confirmed_at, retention_until)
                        VALUES (?, ?, ?, ?, 1, ?, NOW(3), NOW(3), DATE_ADD(NOW(3), INTERVAL 6 MONTH))
                        ON DUPLICATE KEY UPDATE
                            account_name = VALUES(account_name),
                            accepted = 1,
                            consent_source = VALUES(consent_source),
                            last_confirmed_at = NOW(3),
                            retention_until = CASE
                                WHEN retention_until < DATE_ADD(NOW(3), INTERVAL 6 MONTH)
                                    THEN DATE_ADD(NOW(3), INTERVAL 6 MONTH)
                                ELSE retention_until END
                        """, accountId, accountName, AiAssistantPolicy.CONSENT_TYPE,
                AiAssistantPolicy.VERSION, dto.getSource().trim());
        log.info("[平台协议] 后台账户确认协议: accountId={}, version={}, source={}",
                accountId, AiAssistantPolicy.VERSION, dto.getSource());
        return getAiAssistantConsentStatus();
    }

    @Override
    public PageResult<AiAssistantComplaintVO> aiAssistantComplaintPage(AiAssistantComplaintPageQueryDTO query) {
        query.normalize();
        StringBuilder where = new StringBuilder(" FROM ai_assistant_complaint WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(query.getCategory())) {
            where.append(" AND category = ?");
            params.add(normalizeComplaintCategory(query.getCategory()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            where.append(" AND status = ?");
            params.add(normalizeComplaintStatus(query.getStatus()));
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = "%" + query.getKeyword().trim() + "%";
            where.append(" AND (complaint_no LIKE ? OR reporter_name LIKE ? OR content LIKE ?)");
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }
        if (StringUtils.hasText(query.getStartTime())) {
            where.append(" AND created_at >= ?");
            params.add(LocalDate.parse(query.getStartTime()).atStartOfDay());
        }
        if (StringUtils.hasText(query.getEndTime())) {
            where.append(" AND created_at <= ?");
            params.add(LocalDate.parse(query.getEndTime()).atTime(LocalTime.MAX));
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(query.getPageSize());
        pageParams.add((query.getPageNum() - 1) * query.getPageSize());
        List<AiAssistantComplaintVO> records = jdbcTemplate.query("""
                        SELECT id, complaint_no, reporter_id, reporter_name, category, content, request_id, status,
                               handle_reply, handler_id, handler_name, reply_due_at, handled_at, created_at, retention_until
                        """ + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapAiAssistantComplaint(rs), pageParams.toArray());
        return new PageResult<>(total == null ? 0L : total, query.getPageSize(),
                query.getPageNum().longValue(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAssistantComplaintVO handleAiAssistantComplaint(Long id, AiAssistantComplaintHandleDTO dto) {
        AiAssistantComplaintVO existing = findAiAssistantComplaintById(id);
        String status = normalizeComplaintStatus(dto.getStatus());
        String handleReply = StringUtils.hasText(dto.getHandleReply()) ? dto.getHandleReply().trim() : null;
        if (("RESOLVED".equals(status) || "REJECTED".equals(status)) && !StringUtils.hasText(handleReply)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "解决或驳回投诉时必须填写处理答复");
        }
        boolean finalized = "RESOLVED".equals(status) || "REJECTED".equals(status);
        Long handlerId = UserContext.getUserId();
        String handlerName = UserContext.get() != null && StringUtils.hasText(UserContext.get().getUserName())
                ? UserContext.get().getUserName().trim() : "系统管理员";
        jdbcTemplate.update("""
                        UPDATE ai_assistant_complaint
                        SET status = ?, handle_reply = ?, handler_id = ?, handler_name = ?,
                            handled_at = CASE WHEN ? THEN NOW(3) ELSE NULL END
                        WHERE id = ?
                        """, status, handleReply, handlerId, handlerName, finalized, id);
        log.info("[AI投诉举报] 已处理: complaintNo={}, status={}, handlerId={}",
                existing.getComplaintNo(), status, handlerId);
        return findAiAssistantComplaintById(id);
    }

    private AiAssistantComplaintVO findAiAssistantComplaintByNo(String complaintNo) {
        return jdbcTemplate.queryForObject("""
                        SELECT id, complaint_no, reporter_id, reporter_name, category, content, request_id, status,
                               handle_reply, handler_id, handler_name, reply_due_at, handled_at, created_at, retention_until
                        FROM ai_assistant_complaint WHERE complaint_no = ?
                        """, (rs, rowNum) -> mapAiAssistantComplaint(rs), complaintNo);
    }

    private AiAssistantComplaintVO findAiAssistantComplaintById(Long id) {
        List<AiAssistantComplaintVO> records = jdbcTemplate.query("""
                        SELECT id, complaint_no, reporter_id, reporter_name, category, content, request_id, status,
                               handle_reply, handler_id, handler_name, reply_due_at, handled_at, created_at, retention_until
                        FROM ai_assistant_complaint WHERE id = ?
                        """, (rs, rowNum) -> mapAiAssistantComplaint(rs), id);
        if (records.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI投诉或举报记录不存在");
        }
        return records.get(0);
    }

    private AiAssistantComplaintVO mapAiAssistantComplaint(java.sql.ResultSet rs) throws java.sql.SQLException {
        AiAssistantComplaintVO vo = new AiAssistantComplaintVO();
        vo.setId(rs.getLong("id"));
        vo.setComplaintNo(rs.getString("complaint_no"));
        vo.setReporterId(rs.getLong("reporter_id"));
        vo.setReporterName(rs.getString("reporter_name"));
        vo.setCategory(rs.getString("category"));
        vo.setContent(rs.getString("content"));
        vo.setRequestId(rs.getString("request_id"));
        vo.setStatus(rs.getString("status"));
        vo.setHandleReply(rs.getString("handle_reply"));
        long handlerId = rs.getLong("handler_id");
        vo.setHandlerId(rs.wasNull() ? null : handlerId);
        vo.setHandlerName(rs.getString("handler_name"));
        vo.setReplyDueAt(toLocalDateTime(rs.getTimestamp("reply_due_at")));
        vo.setHandledAt(toLocalDateTime(rs.getTimestamp("handled_at")));
        vo.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        vo.setRetentionUntil(toLocalDateTime(rs.getTimestamp("retention_until")));
        return vo;
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long requireCurrentAccountId() {
        Long accountId = UserContext.getUserId();
        if (accountId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return accountId;
    }

    private String normalizeComplaintCategory(String category) {
        String normalized = category == null ? "" : category.trim().toUpperCase();
        if (!AI_COMPLAINT_CATEGORIES.contains(normalized)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的投诉或举报类型");
        }
        return normalized;
    }

    private String normalizeComplaintStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!AI_COMPLAINT_STATUSES.contains(normalized)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的投诉处理状态");
        }
        return normalized;
    }

    private String buildComplaintNo() {
        return "AIC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    // ==================== 角色管理 ====================

    @Override
    public PageResult<SysRoleVO> rolePage(Integer pageNum, Integer pageSize, String keyword) {
        Page<SysRole> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysRole::getRoleName, keyword)
                    .or().like(SysRole::getRoleCode, keyword);
        }
        wrapper.orderByAsc(SysRole::getCreatedAt);
        Page<SysRole> result = sysRoleMapper.selectPage(page, wrapper);
        List<SysRoleVO> voList = result.getRecords().stream()
                .map(SysRoleVO::fromEntity).collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(), voList);
    }

    @Override
    public List<SysRoleVO> listAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, "ENABLED").orderByAsc(SysRole::getCreatedAt);
        return sysRoleMapper.selectList(wrapper).stream()
                .map(SysRoleVO::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO createRole(RoleCreateDTO dto) {
        // 检查编码唯一
        LambdaQueryWrapper<SysRole> check = new LambdaQueryWrapper<>();
        check.eq(SysRole::getRoleCode, dto.getRoleCode());
        if (sysRoleMapper.selectCount(check) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "角色编码已存在: " + dto.getRoleCode());
        }
        SysRole entity = new SysRole();
        entity.setRoleCode(dto.getRoleCode());
        entity.setRoleName(dto.getRoleName());
        entity.setRoleDesc(dto.getRoleDesc());
        entity.setStatus("ENABLED");
        sysRoleMapper.insert(entity);
        log.info("[系统角色] 新建角色: {}", dto.getRoleCode());
        return SysRoleVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO updateRole(Long id, RoleCreateDTO dto) {
        SysRole entity = sysRoleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        entity.setRoleName(dto.getRoleName());
        if (dto.getRoleDesc() != null) entity.setRoleDesc(dto.getRoleDesc());
        sysRoleMapper.updateById(entity);
        log.info("[系统角色] 更新角色: id={}, name={}", id, dto.getRoleName());
        return SysRoleVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole entity = sysRoleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        sysRoleMapper.deleteById(id);
        sysRolePermissionMapper.deleteByRoleId(id);
        log.info("[系统角色] 删除角色: id={}, code={}", id, entity.getRoleCode());
    }

    @Override
    public List<SysPermissionVO> permissionTree() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, "ENABLED")
                .orderByAsc(SysPermission::getParentId)
                .orderByAsc(SysPermission::getSortOrder)
                .orderByAsc(SysPermission::getId);

        List<SysPermissionVO> nodes = sysPermissionMapper.selectList(wrapper).stream()
                .map(SysPermissionVO::fromEntity)
                .collect(Collectors.toList());
        Map<Long, SysPermissionVO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(SysPermissionVO::getId, n -> n, (a, b) -> a, LinkedHashMap::new));
        List<SysPermissionVO> roots = new ArrayList<>();

        for (SysPermissionVO node : nodes) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }

        sortPermissionTree(roots);
        return roots;
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        ensureRoleExists(roleId);
        return sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(Long roleId, RolePermissionUpdateDTO dto) {
        SysRole role = ensureRoleExists(roleId);
        sysRolePermissionMapper.deleteByRoleId(roleId);

        List<Long> permissionIds = dto.getPermissionIds() == null
                ? List.of()
                : dto.getPermissionIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!permissionIds.isEmpty()) {
            List<SysPermission> permissions = sysPermissionMapper.selectBatchIds(permissionIds);
            if (permissions.size() != permissionIds.size()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "存在无效权限ID");
            }

            for (Long permissionId : permissionIds) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                sysRolePermissionMapper.insert(relation);
            }
        }

        log.info("[系统角色] 更新角色权限: roleId={}, code={}, count={}",
                roleId, role.getRoleCode(), permissionIds.size());
    }

    private SysRole ensureRoleExists(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void sortPermissionTree(List<SysPermissionVO> nodes) {
        nodes.sort(Comparator
                .comparing((SysPermissionVO n) -> n.getSortOrder() == null ? 0 : n.getSortOrder())
                .thenComparing(n -> n.getId() == null ? 0L : n.getId()));
        for (SysPermissionVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortPermissionTree(node.getChildren());
            }
        }
    }

    // ==================== 账户管理 ====================

    @Override
    public PageResult<SysAccountVO> accountPage(Integer pageNum, Integer pageSize, String keyword) {
        Page<SysAccount> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<SysAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysAccount::getUsername, keyword)
                    .or().like(SysAccount::getName, keyword)
                    .or().like(SysAccount::getPhone, keyword)
                    .or().like(SysAccount::getEmployeeNo, keyword);
        }
        wrapper.orderByDesc(SysAccount::getCreatedAt);
        Page<SysAccount> result = sysAccountMapper.selectPage(page, wrapper);

        // 批量查询角色名
        List<Long> roleIds = result.getRecords().stream()
                .map(SysAccount::getRoleId).distinct().filter(r -> r != null).collect(Collectors.toList());
        Map<Long, String> roleNameMap = Map.of();
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            roleNameMap = roles.stream().collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));
        }

        final Map<Long, String> finalRoleNameMap = roleNameMap;
        List<SysAccountVO> voList = result.getRecords().stream().map(e -> {
            SysAccountVO vo = SysAccountVO.fromEntity(e);
            if (e.getRoleId() != null) vo.setRoleName(finalRoleNameMap.get(e.getRoleId()));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAccountVO createAccount(AccountCreateDTO dto) {
        if (dto.getRoleId() == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "\u8bf7\u9009\u62e9\u89d2\u8272");
        }
        SysRole role = ensureRoleExists(dto.getRoleId());
        if (!"ENABLED".equals(role.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "\u6240\u9009\u89d2\u8272\u5df2\u7981\u7528");
        }

        // 检查用户名唯一
        LambdaQueryWrapper<SysAccount> check = new LambdaQueryWrapper<>();
        check.eq(SysAccount::getUsername, dto.getUsername());
        if (sysAccountMapper.selectCount(check) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "用户名已存在，请更换后再添加: " + dto.getUsername());
        }
        check = new LambdaQueryWrapper<>();
        check.eq(SysAccount::getEmployeeNo, dto.getEmployeeNo());
        if (sysAccountMapper.selectCount(check) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "工号已存在，请更换后再添加: " + dto.getEmployeeNo());
        }
        SysAccount entity = new SysAccount();
        entity.setEmployeeNo(dto.getEmployeeNo());
        entity.setUsername(dto.getUsername());
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        entity.setRoleId(dto.getRoleId());
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setDepartment(dto.getDepartment());
        entity.setStatus("ENABLED");
        try {
            sysAccountMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "用户名或工号已存在，请更换后再添加");
        }
        log.info("[系统账户] 新增账户: {}", dto.getUsername());
        return SysAccountVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAccountVO updateAccount(Long id, AccountUpdateDTO dto) {
        SysAccount entity = sysAccountMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账户不存在");
        }
        boolean identityDetailsChanged = !Objects.equals(entity.getName(), dto.getName())
                || !Objects.equals(entity.getPhone(), dto.getPhone());
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setDepartment(dto.getDepartment());
        if (dto.getRoleId() != null) {
            SysRole role = ensureRoleExists(dto.getRoleId());
            if (!"ENABLED".equals(role.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "\u6240\u9009\u89d2\u8272\u5df2\u7981\u7528");
            }
            entity.setRoleId(dto.getRoleId());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (Boolean.TRUE.equals(entity.getIdentityVerified()) && identityDetailsChanged) {
            entity.setIdentityVerified(false);
            entity.setIdentityVerifiedAt(null);
            entity.setIdentityVerifiedBy(null);
            entity.setIdentityVerificationMethod(IDENTITY_INVALIDATED_METHOD);
        }
        sysAccountMapper.updateById(entity);
        if (identityDetailsChanged && IDENTITY_INVALIDATED_METHOD.equals(entity.getIdentityVerificationMethod())) {
            recordIdentityVerification(entity, false, IDENTITY_INVALIDATED_METHOD, UserContext.getUserId());
        }
        log.info("[系统账户] 更新账户: id={}, username={}", id, entity.getUsername());
        return SysAccountVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAccountVO updateAccountIdentityVerification(Long id, AccountIdentityVerificationDTO dto) {
        SysAccount entity = sysAccountMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账户不存在");
        }

        Long operatorId = UserContext.getUserId();
        if (Boolean.TRUE.equals(dto.getVerified())) {
            validateIdentityDetails(entity);
            entity.setIdentityVerified(true);
            entity.setIdentityVerifiedAt(LocalDateTime.now());
            entity.setIdentityVerifiedBy(operatorId);
            entity.setIdentityVerificationMethod(IDENTITY_VERIFICATION_METHOD);
        } else {
            entity.setIdentityVerified(false);
            entity.setIdentityVerifiedAt(null);
            entity.setIdentityVerifiedBy(null);
            entity.setIdentityVerificationMethod(null);
        }
        sysAccountMapper.updateById(entity);
        recordIdentityVerification(entity, Boolean.TRUE.equals(dto.getVerified()),
                Boolean.TRUE.equals(dto.getVerified()) ? IDENTITY_VERIFICATION_METHOD : "MANUAL_REVOKE", operatorId);
        log.info("[系统账户] 身份核验状态更新: id={}, username={}, verified={}, operatorId={}",
                id, entity.getUsername(), dto.getVerified(), operatorId);
        return SysAccountVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long id) {
        SysAccount entity = sysAccountMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账户不存在");
        }
        ensureNotLastEnabledSuperAdmin(entity);
        removeAccountAndRetainAiRecords(entity);
        revokeAccountTokens(id);
        log.info("[系统账户] 管理员删除账户: id={}, username={}", id, entity.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCurrentAccount(AccountSelfCancellationDTO dto) {
        Long accountId = UserContext.getUserId();
        String userType = UserContext.getUserType();
        if (accountId == null || !BACKEND_ACCOUNT_USER_TYPES.contains(userType)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅后台账户可申请注销");
        }
        if (!ACCOUNT_CANCELLATION_CONFIRM_TEXT.equals(dto.getConfirmText().trim())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "请输入“注销账户”确认");
        }

        SysAccount entity = sysAccountMapper.selectById(accountId);
        if (entity == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND, "当前账户不存在或已注销");
        }
        if (!passwordEncoder.matches(dto.getPassword(), entity.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "当前账户密码不正确");
        }
        ensureNotLastEnabledSuperAdmin(entity);
        removeAccountAndRetainAiRecords(entity);
        revokeAccountTokens(accountId);
        log.info("[系统账户] 账户自助注销: id={}, username={}, userType={}",
                accountId, entity.getUsername(), userType);
    }

    private void removeAccountAndRetainAiRecords(SysAccount entity) {
        Long accountId = entity.getId();
        sysAccountMapper.deleteById(accountId);
        // Keep minimal AI audit records for at least six months after account deletion.
        // This shares the transaction with deletion: a failed retention hold prevents premature account removal.
        jdbcTemplate.update("""
                        UPDATE ai_assistant_audit_log
                        SET account_cancelled_at = NOW(3), retention_until = CASE
                            WHEN retention_until < DATE_ADD(NOW(3), INTERVAL 6 MONTH) THEN DATE_ADD(NOW(3), INTERVAL 6 MONTH)
                        ELSE retention_until END
                        WHERE operator_id = ?
                        """, accountId);
        jdbcTemplate.update("""
                        UPDATE ai_assistant_complaint
                        SET account_cancelled_at = NOW(3), retention_until = CASE
                            WHEN retention_until < DATE_ADD(NOW(3), INTERVAL 6 MONTH) THEN DATE_ADD(NOW(3), INTERVAL 6 MONTH)
                        ELSE retention_until END
                        WHERE reporter_id = ?
                        """, accountId);
        jdbcTemplate.update("""
                        UPDATE ai_assistant_consent_log
                        SET account_cancelled_at = NOW(3), retention_until = CASE
                            WHEN retention_until < DATE_ADD(NOW(3), INTERVAL 6 MONTH) THEN DATE_ADD(NOW(3), INTERVAL 6 MONTH)
                        ELSE retention_until END
                        WHERE account_id = ?
                        """, accountId);
    }

    private void ensureNotLastEnabledSuperAdmin(SysAccount entity) {
        SysRole role = entity.getRoleId() == null ? null : sysRoleMapper.selectById(entity.getRoleId());
        if (role == null || !"SUPER_ADMIN".equals(role.getRoleCode()) || !"ENABLED".equals(entity.getStatus())) {
            return;
        }
        Long remainingCount = sysAccountMapper.selectCount(new LambdaQueryWrapper<SysAccount>()
                .eq(SysAccount::getRoleId, entity.getRoleId())
                .eq(SysAccount::getStatus, "ENABLED")
                .ne(SysAccount::getId, entity.getId()));
        if (remainingCount == null || remainingCount == 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "系统至少需保留一名启用的超级管理员，请先创建或启用其他超级管理员账户");
        }
    }

    private void revokeAccountTokens(Long accountId) {
        for (String userType : TOKEN_USER_TYPES) {
            String identityKey = userType + ":" + accountId;
            stringRedisTemplate.delete(REDIS_ACCESS_TOKEN_KEY + identityKey);
            stringRedisTemplate.delete(REDIS_REFRESH_TOKEN_KEY + identityKey);
            stringRedisTemplate.delete(REDIS_SESSION_KEY + identityKey);
        }
        // Clear legacy token keys kept by older gateway versions as well.
        stringRedisTemplate.delete(REDIS_ACCESS_TOKEN_KEY + accountId);
        stringRedisTemplate.delete(REDIS_REFRESH_TOKEN_KEY + accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        SysAccount entity = sysAccountMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账户不存在");
        }
        entity.setPassword(passwordEncoder.encode(newPassword));
        sysAccountMapper.updateById(entity);
        log.info("[系统账户] 重置密码: id={}, username={}", id, entity.getUsername());
    }

    private void validateIdentityDetails(SysAccount entity) {
        if (!StringUtils.hasText(entity.getName())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "身份核验需要填写真实姓名");
        }
        if (!StringUtils.hasText(entity.getPhone())
                || !CHINA_MOBILE_PATTERN.matcher(entity.getPhone().trim()).matches()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "身份核验需要填写有效的11位手机号");
        }
    }

    private void recordIdentityVerification(SysAccount account, boolean verified, String method, Long operatorId) {
        jdbcTemplate.update("""
                        INSERT INTO sys_user_identity_verification_log
                            (account_id, verified, verification_method, verified_by, verified_at, account_name, phone_masked)
                        VALUES (?, ?, ?, ?, NOW(3), ?, ?)
                        """,
                account.getId(), verified, method, operatorId, account.getName(), maskPhone(account.getPhone()));
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String normalized = phone.trim();
        if (normalized.length() == 11) {
            return normalized.substring(0, 3) + "****" + normalized.substring(7);
        }
        return "已绑定";
    }

    // ===== MQTT 配置管理 =====

    private static final String MQTT_KEY_NAME = "mqtt.name";
    private static final String MQTT_KEY_BROKER = "mqtt.broker";
    private static final String MQTT_KEY_CLIENT_ID = "mqtt.client_id";
    private static final String MQTT_KEY_USERNAME = "mqtt.username";
    private static final String MQTT_KEY_PASSWORD = "mqtt.password";
    private static final String MQTT_KEY_KEEPALIVE = "mqtt.keep_alive_interval";
    private static final String MQTT_KEY_CLEAN_SESSION = "mqtt.clean_session";
    private static final String MQTT_KEY_ENABLED = "mqtt.enabled";

    /**
     * 从 sys_config 读取所有 mqtt.* 配置并组装为 MqttConfigVO
     * 关联信息（运行/链路状态）从 iot-service 实时拉取
     */
    @Override
    public MqttConfigVO getMqttConfig() {
        MqttConfigVO vo = new MqttConfigVO();
        List<SysConfig> all = sysConfigMapper.selectList(null);
        for (SysConfig cfg : all) {
            switch (cfg.getConfigKey()) {
                case MQTT_KEY_NAME -> { vo.setId(cfg.getId()); vo.setName(cfg.getConfigValue()); vo.setCreatedAt(cfg.getCreatedAt()); vo.setUpdatedAt(cfg.getUpdatedAt()); }
                case MQTT_KEY_BROKER -> vo.setBroker(cfg.getConfigValue());
                case MQTT_KEY_CLIENT_ID -> vo.setClientId(cfg.getConfigValue());
                case MQTT_KEY_USERNAME -> vo.setUsername(cfg.getConfigValue());
                case MQTT_KEY_PASSWORD -> vo.setPassword(cfg.getConfigValue());
                case MQTT_KEY_KEEPALIVE -> {
                    try { vo.setKeepAliveInterval(Integer.parseInt(cfg.getConfigValue())); } catch (Exception ignored) {}
                }
                case MQTT_KEY_CLEAN_SESSION -> vo.setCleanSession(Boolean.parseBoolean(cfg.getConfigValue()));
                case MQTT_KEY_ENABLED -> vo.setEnabled(Boolean.parseBoolean(cfg.getConfigValue()));
                default -> {}
            }
        }
        // 默认值兜底
        if (vo.getName() == null) vo.setName("MQTT服务器");
        if (vo.getKeepAliveInterval() == null) vo.setKeepAliveInterval(60);
        if (vo.getCleanSession() == null) vo.setCleanSession(false);
        if (vo.getEnabled() == null) vo.setEnabled(true);

        // 拉取 iot-service 实时状态
        try {
            var statusRes = iotServiceClient.mqttStatus();
            if (statusRes != null && statusRes.getCode() == 200 && statusRes.getData() != null) {
                Map<String, Object> st = statusRes.getData();
                vo.setRunningStatus(Boolean.TRUE.equals(st.get("running")) ? "运行" : "停止");
                vo.setLinkStatus(Boolean.TRUE.equals(st.get("connected")) ? "连接" : "断开");
            } else {
                vo.setRunningStatus("未知");
                vo.setLinkStatus("未知");
            }
        } catch (Exception e) {
            log.warn("获取 iot-service MQTT 状态失败: {}", e.getMessage());
            vo.setRunningStatus("未知");
            vo.setLinkStatus("未知");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MqttConfigVO updateMqttConfig(MqttConfigUpdateDTO dto) {
        // 批量更新 8 个配置项
        upsertConfig(MQTT_KEY_NAME, dto.getName(), "MQTT连接配置名称");
        upsertConfig(MQTT_KEY_BROKER, dto.getBroker(), "MQTT Broker 地址");
        upsertConfig(MQTT_KEY_CLIENT_ID, dto.getClientId() == null ? "iot-service" : dto.getClientId(), "MQTT 客户端ID");
        upsertConfig(MQTT_KEY_USERNAME, dto.getUsername() == null ? "" : dto.getUsername(), "MQTT 用户名");
        upsertConfig(MQTT_KEY_PASSWORD, dto.getPassword() == null ? "" : dto.getPassword(), "MQTT 密码");
        upsertConfig(MQTT_KEY_KEEPALIVE, String.valueOf(dto.getKeepAliveInterval() == null ? 60 : dto.getKeepAliveInterval()), "心跳间隔(秒)");
        upsertConfig(MQTT_KEY_CLEAN_SESSION, String.valueOf(dto.getCleanSession() == null ? false : dto.getCleanSession()), "清除会话");
        upsertConfig(MQTT_KEY_ENABLED, String.valueOf(dto.getEnabled() == null ? true : dto.getEnabled()), "是否启用");
        log.info("[MQTT配置] 更新完成: broker={}, enabled={}", dto.getBroker(), dto.getEnabled());

        // 在事务提交后通知 iot-service 重新加载并重连
        // （如果在事务内直接调用 Feign，iot-service 读取 sys_config 时事务尚未提交，会拿到旧配置）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    iotServiceClient.reloadMqtt();
                    log.info("[MQTT配置] 已通知 iot-service 重新加载");
                } catch (Exception e) {
                    log.warn("通知 iot-service 重连失败: {}", e.getMessage());
                }
            }
        });
        return getMqttConfig();
    }

    @Override
    public Map<String, Object> testMqttConnection(MqttConfigUpdateDTO dto) {
        // 这里只做简单的格式校验，真实连接由 iot-service 处理
        Map<String, Object> result = new java.util.HashMap<>();
        if (dto.getBroker() == null || !dto.getBroker().startsWith("tcp://")) {
            result.put("success", false);
            result.put("message", "Broker 地址格式错误，必须以 tcp:// 开头");
            return result;
        }
        result.put("success", true);
        result.put("message", "配置格式校验通过，保存后将自动重连");
        return result;
    }

    private void upsertConfig(String key, String value, String desc) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig entity = sysConfigMapper.selectOne(wrapper);
        if (entity == null) {
            entity = new SysConfig();
            entity.setConfigKey(key);
            entity.setConfigValue(value);
            entity.setConfigDesc(desc);
            sysConfigMapper.insert(entity);
        } else {
            entity.setConfigValue(value);
            sysConfigMapper.updateById(entity);
        }
    }
}
