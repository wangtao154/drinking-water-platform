package com.platform.system.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.common.auth.RequirePermission;
import com.platform.system.dto.AccountCreateDTO;
import com.platform.system.dto.AccountUpdateDTO;
import com.platform.system.dto.AuditLogPageQueryDTO;
import com.platform.system.dto.ConfigUpdateDTO;
import com.platform.system.dto.MqttConfigUpdateDTO;
import com.platform.system.dto.RoleCreateDTO;
import com.platform.system.dto.RolePermissionUpdateDTO;
import com.platform.system.service.SystemService;
import com.platform.system.vo.AuditLogVO;
import com.platform.system.vo.MqttConfigVO;
import com.platform.system.vo.SysAccountVO;
import com.platform.system.vo.SysConfigVO;
import com.platform.system.vo.SysPermissionVO;
import com.platform.system.vo.SysRoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理 Controller
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    // ===== 系统配置 =====

    @GetMapping("/configs")
    @RequirePermission("SYSTEM_CONFIG")
    public R<List<SysConfigVO>> listConfigs() {
        return R.ok(systemService.listConfigs());
    }

    @GetMapping("/configs/{key}")
    @RequirePermission("SYSTEM_CONFIG")
    public R<SysConfigVO> getConfigByKey(@PathVariable String key) {
        return R.ok(systemService.getConfigByKey(key));
    }

    @PutMapping("/configs/{id}")
    @RequirePermission("SYSTEM_CONFIG")
    public R<SysConfigVO> updateConfig(@PathVariable Long id, @Valid @RequestBody ConfigUpdateDTO dto) {
        return R.ok(systemService.updateConfig(id, dto));
    }

    // ===== 审计日志 =====

    @GetMapping("/audit-logs")
    @RequirePermission("SYSTEM_AUDIT")
    public R<PageResult<AuditLogVO>> auditLogPage(AuditLogPageQueryDTO query) {
        return R.ok(systemService.auditLogPage(query));
    }

    // ===== 角色管理 =====

    @GetMapping("/roles")
    @RequirePermission("SYSTEM_ROLE")
    public R<PageResult<SysRoleVO>> rolePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(systemService.rolePage(pageNum, pageSize, keyword));
    }

    @GetMapping("/roles/list")
    @RequirePermission({"SYSTEM_ROLE", "SYSTEM_USER"})
    public R<List<SysRoleVO>> listAllRoles() {
        return R.ok(systemService.listAllRoles());
    }

    @PostMapping("/roles")
    @RequirePermission("SYSTEM_ROLE")
    public R<SysRoleVO> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        return R.ok(systemService.createRole(dto));
    }

    @PutMapping("/roles/{id}")
    @RequirePermission("SYSTEM_ROLE")
    public R<SysRoleVO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleCreateDTO dto) {
        return R.ok(systemService.updateRole(id, dto));
    }

    @DeleteMapping("/roles/{id}")
    @RequirePermission("SYSTEM_ROLE")
    public R<Void> deleteRole(@PathVariable Long id) {
        systemService.deleteRole(id);
        return R.ok(null);
    }

    @GetMapping("/permissions/tree")
    @RequirePermission("SYSTEM_ROLE")
    public R<List<SysPermissionVO>> permissionTree() {
        return R.ok(systemService.permissionTree());
    }

    @GetMapping("/roles/{id}/permissions")
    @RequirePermission("SYSTEM_ROLE")
    public R<List<Long>> getRolePermissionIds(@PathVariable Long id) {
        return R.ok(systemService.getRolePermissionIds(id));
    }

    @PutMapping("/roles/{id}/permissions")
    @RequirePermission("SYSTEM_ROLE_PERMISSION")
    public R<Void> updateRolePermissions(@PathVariable Long id, @Valid @RequestBody RolePermissionUpdateDTO dto) {
        systemService.updateRolePermissions(id, dto);
        return R.ok(null);
    }

    // ===== 账户管理 =====

    @GetMapping("/users")
    @RequirePermission("SYSTEM_USER")
    public R<PageResult<SysAccountVO>> accountPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(systemService.accountPage(pageNum, pageSize, keyword));
    }

    @PostMapping("/users")
    @RequirePermission("SYSTEM_USER")
    public R<SysAccountVO> createAccount(@Valid @RequestBody AccountCreateDTO dto) {
        return R.ok(systemService.createAccount(dto));
    }

    @PutMapping("/users/{id}")
    @RequirePermission("SYSTEM_USER")
    public R<SysAccountVO> updateAccount(@PathVariable Long id, @RequestBody AccountUpdateDTO dto) {
        return R.ok(systemService.updateAccount(id, dto));
    }

    @DeleteMapping("/users/{id}")
    @RequirePermission("SYSTEM_USER")
    public R<Void> deleteAccount(@PathVariable Long id) {
        systemService.deleteAccount(id);
        return R.ok(null);
    }

    @PutMapping("/users/{id}/reset-password")
    @RequirePermission("SYSTEM_USER")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        systemService.resetPassword(id, body.get("password"));
        return R.ok(null);
    }

    // ===== MQTT 配置管理 =====

    /** 获取聚合后的 MQTT 配置（含 iot-service 实时运行/链路状态） */
    @GetMapping("/mqtt-config")
    @RequirePermission("SYSTEM_CONFIG")
    public R<MqttConfigVO> getMqttConfig() {
        return R.ok(systemService.getMqttConfig());
    }

    /** 一次性更新所有 MQTT 配置项，并通知 iot-service 重连 */
    @PutMapping("/mqtt-config")
    @RequirePermission("SYSTEM_CONFIG")
    public R<MqttConfigVO> updateMqttConfig(@Valid @RequestBody MqttConfigUpdateDTO dto) {
        return R.ok(systemService.updateMqttConfig(dto));
    }

    /** 测试 MQTT 配置（仅格式校验） */
    @PostMapping("/mqtt-config/test")
    @RequirePermission("SYSTEM_CONFIG")
    public R<Map<String, Object>> testMqttConnection(@RequestBody MqttConfigUpdateDTO dto) {
        return R.ok(systemService.testMqttConnection(dto));
    }
}
