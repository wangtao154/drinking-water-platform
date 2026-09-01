package com.platform.system.service;

import com.platform.common.result.PageResult;
import com.platform.system.dto.AuditLogPageQueryDTO;
import com.platform.system.dto.AiAssistantAuditLogPageQueryDTO;
import com.platform.system.dto.AiAssistantComplaintCreateDTO;
import com.platform.system.dto.AiAssistantComplaintHandleDTO;
import com.platform.system.dto.AiAssistantComplaintPageQueryDTO;
import com.platform.system.dto.AiAssistantConsentAcceptDTO;
import com.platform.system.dto.ConfigUpdateDTO;
import com.platform.system.dto.AccountCreateDTO;
import com.platform.system.dto.AccountSelfCancellationDTO;
import com.platform.system.dto.AccountUpdateDTO;
import com.platform.system.dto.AccountIdentityVerificationDTO;
import com.platform.system.dto.MqttConfigUpdateDTO;
import com.platform.system.dto.RoleCreateDTO;
import com.platform.system.dto.RolePermissionUpdateDTO;
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

import java.util.List;
import java.util.Map;

/**
 * 系统管理 Service
 */
public interface SystemService {

    // ===== 系统配置 =====
    List<SysConfigVO> listConfigs();
    SysConfigVO getConfigByKey(String key);
    SysConfigVO updateConfig(Long id, ConfigUpdateDTO dto);

    // ===== 审计日志 =====
    PageResult<AuditLogVO> auditLogPage(AuditLogPageQueryDTO query);
    PageResult<AiAssistantAuditLogVO> aiAssistantAuditLogPage(AiAssistantAuditLogPageQueryDTO query);
    AiAssistantAuditIntegrityVO verifyAiAssistantAuditIntegrity();
    AiAssistantComplaintVO createAiAssistantComplaint(AiAssistantComplaintCreateDTO dto);
    PageResult<AiAssistantComplaintVO> aiAssistantComplaintPage(AiAssistantComplaintPageQueryDTO query);
    AiAssistantComplaintVO handleAiAssistantComplaint(Long id, AiAssistantComplaintHandleDTO dto);
    AiAssistantConsentStatusVO getAiAssistantConsentStatus();
    AiAssistantConsentStatusVO acceptAiAssistantConsent(AiAssistantConsentAcceptDTO dto);

    // ===== 角色管理 =====
    PageResult<SysRoleVO> rolePage(Integer pageNum, Integer pageSize, String keyword);
    List<SysRoleVO> listAllRoles();
    SysRoleVO createRole(RoleCreateDTO dto);
    SysRoleVO updateRole(Long id, RoleCreateDTO dto);
    void deleteRole(Long id);
    List<SysPermissionVO> permissionTree();
    List<Long> getRolePermissionIds(Long roleId);
    void updateRolePermissions(Long roleId, RolePermissionUpdateDTO dto);

    // ===== 账户管理 =====
    PageResult<SysAccountVO> accountPage(Integer pageNum, Integer pageSize, String keyword);
    SysAccountVO createAccount(AccountCreateDTO dto);
    SysAccountVO updateAccount(Long id, AccountUpdateDTO dto);
    SysAccountVO updateAccountIdentityVerification(Long id, AccountIdentityVerificationDTO dto);
    void deleteAccount(Long id);
    void cancelCurrentAccount(AccountSelfCancellationDTO dto);
    void resetPassword(Long id, String newPassword);

    // ===== MQTT 配置管理 =====
    /** 获取聚合后的 MQTT 配置（含运行状态） */
    MqttConfigVO getMqttConfig();

    /** 一次性更新所有 MQTT 配置项（事务），并通过 Feign 通知 iot-service 重连 */
    MqttConfigVO updateMqttConfig(MqttConfigUpdateDTO dto);

    /** 测试连接（尝试连接 broker） */
    Map<String, Object> testMqttConnection(MqttConfigUpdateDTO dto);
}
