package com.platform.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.system.dto.AuditLogPageQueryDTO;
import com.platform.system.dto.AccountCreateDTO;
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
import com.platform.system.vo.MqttConfigVO;
import com.platform.system.vo.SysAccountVO;
import com.platform.system.vo.SysConfigVO;
import com.platform.system.vo.SysPermissionVO;
import com.platform.system.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 系统管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final SysConfigMapper sysConfigMapper;
    private final AuditLogMapper auditLogMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysAccountMapper sysAccountMapper;
    private final IotServiceClient iotServiceClient;
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
        sysAccountMapper.updateById(entity);
        log.info("[系统账户] 更新账户: id={}, username={}", id, entity.getUsername());
        return SysAccountVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long id) {
        SysAccount entity = sysAccountMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账户不存在");
        }
        sysAccountMapper.deleteById(id);
        log.info("[系统账户] 删除账户: id={}, username={}", id, entity.getUsername());
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
