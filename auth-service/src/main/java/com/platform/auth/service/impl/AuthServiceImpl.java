package com.platform.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.auth.dto.ChangePasswordDTO;
import com.platform.auth.dto.CustomerChangePasswordDTO;
import com.platform.auth.dto.CustomerLoginDTO;
import com.platform.auth.dto.LoginDTO;
import com.platform.auth.dto.WxLoginDTO;
import com.platform.auth.entity.Customer;
import com.platform.auth.entity.SysPermission;
import com.platform.auth.entity.SysRole;
import com.platform.auth.entity.SysUser;
import com.platform.auth.entity.Worker;
import com.platform.auth.mapper.CustomerMapper;
import com.platform.auth.mapper.SysPermissionMapper;
import com.platform.auth.mapper.SysRoleMapper;
import com.platform.auth.mapper.SysUserMapper;
import com.platform.auth.mapper.WorkerMapper;
import com.platform.auth.service.AuthService;
import com.platform.auth.vo.LoginVO;
import com.platform.auth.vo.UserInfoVO;
import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.JwtUtil;
import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 认证授权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final CustomerMapper customerMapper;
    private final WorkerMapper workerMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${wechat.miniapp.appid:}")
    private String miniAppId;

    @Value("${wechat.miniapp.appsecret:}")
    private String miniAppSecret;

    @Value("${wechat.miniapp.mock:true}")
    private Boolean miniAppMock;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String REDIS_ACCESS_TOKEN_KEY = "auth:token:access:";
    private static final String REDIS_REFRESH_TOKEN_KEY = "auth:token:refresh:";
    private static final String REDIS_SESSION_KEY = "auth:session:";

    private TokenPair issueTokens(CurrentUser currentUser) {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtUtil.createAccessToken(currentUser, sessionId);
        String refreshToken = jwtUtil.createRefreshToken(currentUser, sessionId);
        long accessExpire = jwtUtil.getAccessTokenExpire();
        long refreshExpire = jwtUtil.getRefreshTokenExpire();
        String identityKey = identityKey(currentUser.getUserType(), currentUser.getUserId());

        stringRedisTemplate.opsForValue().set(
                REDIS_ACCESS_TOKEN_KEY + identityKey, accessToken, Duration.ofSeconds(accessExpire));
        stringRedisTemplate.opsForValue().set(
                REDIS_REFRESH_TOKEN_KEY + identityKey, refreshToken, Duration.ofSeconds(refreshExpire));
        stringRedisTemplate.opsForValue().set(
                REDIS_SESSION_KEY + identityKey, sessionId, Duration.ofSeconds(refreshExpire));

        return new TokenPair(accessToken, refreshToken, sessionId, accessExpire);
    }

    private void storeAccessToken(CurrentUser currentUser, String accessToken, long accessExpire) {
        stringRedisTemplate.opsForValue().set(
                REDIS_ACCESS_TOKEN_KEY + identityKey(currentUser.getUserType(), currentUser.getUserId()),
                accessToken,
                Duration.ofSeconds(accessExpire)
        );
    }

    private void deleteIdentityTokens(String userType, Long userId) {
        if (userId == null || userType == null || userType.isBlank()) {
            return;
        }
        String identityKey = identityKey(userType, userId);
        stringRedisTemplate.delete(REDIS_ACCESS_TOKEN_KEY + identityKey);
        stringRedisTemplate.delete(REDIS_REFRESH_TOKEN_KEY + identityKey);
        stringRedisTemplate.delete(REDIS_SESSION_KEY + identityKey);
    }

    private void deletePossibleIdentityTokens(Long userId) {
        if (userId == null) {
            return;
        }
        List.of("SUPER_ADMIN", "ADMIN", "OPERATOR", "FINANCE", "CUSTOMER", "GUEST", "WORKER")
                .forEach(userType -> deleteIdentityTokens(userType, userId));
        stringRedisTemplate.delete(REDIS_ACCESS_TOKEN_KEY + userId);
        stringRedisTemplate.delete(REDIS_REFRESH_TOKEN_KEY + userId);
    }

    private String identityKey(String userType, Long userId) {
        return userType + ":" + userId;
    }

    private record TokenPair(String accessToken, String refreshToken, String sessionId, long accessExpire) {
    }

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        // 1. 查询用户
        SysUser user = sysUserMapper.selectByAccount(dto.getAccount());
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        // 2. 密码校验
        if (!bCryptPasswordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 3. 状态校验
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 4. 查询角色
        SysRole role = sysRoleMapper.selectByUserId(user.getId());
        if (role == null || !"ENABLED".equals(role.getStatus())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "角色未分配或已禁用");
        }

        // 5. 查询权限
        List<SysPermission> permissions = sysPermissionMapper.selectByRoleId(role.getId());
        Set<String> permissionCodes = permissions.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toSet());

        // 超级管理员拥有全部权限标记
        String userType = role.getRoleCode();
        if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            permissionCodes.add("*");
        }

        // 6. 构建 CurrentUser
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(user.getId());
        currentUser.setUserName(user.getName() != null ? user.getName() : user.getUsername());
        currentUser.setUserType(userType);
        currentUser.setPermissions(permissionCodes);

        // 7. 生成 JWT
        TokenPair tokens = issueTokens(currentUser);

        // 8. 存入 Redis

        // 9. 更新最后登录信息
        sysUserMapper.updateLastLogin(user.getId(), clientIp);

        // 10. 构建返回
        UserInfoVO userInfo = UserInfoVO.builder()
                .userId(user.getId())
                .userName(currentUser.getUserName())
                .userType(userType)
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .permissions(permissionCodes)
                .build();

        log.info("用户登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), role.getRoleCode());

        return LoginVO.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresIn(tokens.accessExpire())
                .userInfo(userInfo)
                .build();
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 1. 验证 refreshToken
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "refreshToken 无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String userType = jwtUtil.getUserTypeFromToken(refreshToken);
        String sessionId = jwtUtil.getSessionIdFromToken(refreshToken);
        if (userId == null || userType == null || sessionId == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "refreshToken 已失效，请重新登录");
        }

        String identityKey = identityKey(userType, userId);
        String storedToken = stringRedisTemplate.opsForValue().get(REDIS_REFRESH_TOKEN_KEY + identityKey);
        String storedSessionId = stringRedisTemplate.opsForValue().get(REDIS_SESSION_KEY + identityKey);
        if (!sessionId.equals(storedSessionId)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "账号已在其他设备登录，请重新登录");
        }

        // 2. 检查 Redis 中的 refreshToken 是否一致
        // Verify refreshToken against the current identity-scoped Redis session.
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "refreshToken 已失效，请重新登录");
        }

        // 3. 重新查询用户信息
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        SysRole role = sysRoleMapper.selectByUserId(userId);
        if (role == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "角色未分配");
        }

        List<SysPermission> permissions = sysPermissionMapper.selectByRoleId(role.getId());
        Set<String> permissionCodes = permissions.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toSet());
        if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            permissionCodes.add("*");
        }

        // 4. 生成新的 accessToken
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(user.getId());
        currentUser.setUserName(user.getName() != null ? user.getName() : user.getUsername());
        currentUser.setUserType(role.getRoleCode());
        currentUser.setPermissions(permissionCodes);

        String newAccessToken = jwtUtil.createAccessToken(currentUser, sessionId);
        long accessExpire = jwtUtil.getAccessTokenExpire();

        // 5. 更新 Redis 中的 accessToken
        storeAccessToken(currentUser, newAccessToken, accessExpire);

        UserInfoVO userInfo = UserInfoVO.builder()
                .userId(user.getId())
                .userName(currentUser.getUserName())
                .userType(role.getRoleCode())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .permissions(permissionCodes)
                .build();

        return LoginVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)  // refreshToken 不变
                .expiresIn(accessExpire)
                .userInfo(userInfo)
                .build();
    }

    @Override
    public void logout(Long userId, String accessToken) {
        // 删除 Redis 中的 token
        deleteIdentityTokens(jwtUtil.getUserTypeFromToken(accessToken), userId);
        log.info("用户登出: userId={}", userId);
    }

    @Override
    public UserInfoVO getCurrentUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        SysRole role = sysRoleMapper.selectByUserId(userId);
        List<SysPermission> permissions = role != null
                ? sysPermissionMapper.selectByRoleId(role.getId())
                : List.of();

        Set<String> permissionCodes = permissions.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toSet());
        if (role != null && "SUPER_ADMIN".equals(role.getRoleCode())) {
            permissionCodes.add("*");
        }

        return UserInfoVO.builder()
                .userId(user.getId())
                .userName(user.getName() != null ? user.getName() : user.getUsername())
                .userType(role != null ? role.getRoleCode() : "UNKNOWN")
                .roleCode(role != null ? role.getRoleCode() : null)
                .roleName(role != null ? role.getRoleName() : null)
                .permissions(permissionCodes)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        // 验证原密码
        if (!bCryptPasswordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码不正确");
        }

        // 更新密码
        String encodedPassword = bCryptPasswordEncoder.encode(dto.getNewPassword());
        user.setPassword(encodedPassword);
        sysUserMapper.updateById(user);

        // 清除 Redis 中的 token，强制重新登录
        deletePossibleIdentityTokens(userId);

        log.info("用户修改密码: userId={}", userId);
    }

    /**
     * 微信小程序登录（Mock 模式）
     * 无真实 AppID/AppSecret 时，用 code 生成模拟 openid 实现完整登录流程
     *
     * 身份判断逻辑：
     * 1. 先查 worker 表（by openId）→ 如果找到，说明已审批为运维人员，返回 WORKER
     * 2. 再查 customer 表（by openId）→ 如果找到，根据 status 判断：
     *    - GUEST → 游客（已注册但未审批）
     *    - ACTIVE → 正式客户
     * 3. 都没找到 → 自动注册 customer（status=GUEST），返回 GUEST
     */
    @Override
    @Transactional
    public LoginVO wxLogin(WxLoginDTO dto, String clientIp) {
        // 1. Mock 模式：使用小程序端生成的稳定 mockOpenId（每个设备唯一且不变）
        // TODO: 上线前替换为真实微信 code2session API 调用（code → openid）
        MiniAppSession miniSession = resolveMiniAppSession(dto);
        String openId = miniSession.openId();

        // 2. 先检查是否已是运维人员（通过 openId 关联 worker 表）
        Worker worker = workerMapper.selectByOpenId(openId);
        if (worker != null) {
            // --- 运维人员登录 ---
            String status = worker.getStatus();
            if (status != null && ("DISABLED".equals(status) || "CANCELLED".equals(status))) {
                throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用");
            }

            CurrentUser currentUser = new CurrentUser();
            currentUser.setUserId(worker.getId());
            currentUser.setUserName(worker.getName() != null ? worker.getName() : "运维人员");
            currentUser.setUserType("WORKER");
            currentUser.setDealerId(worker.getDealerId());
            currentUser.setPermissions(new HashSet<>());

            TokenPair tokens = issueTokens(currentUser);

            UserInfoVO userInfo = UserInfoVO.builder()
                    .userId(worker.getId())
                    .userName(currentUser.getUserName())
                    .userType("WORKER")
                    .roleCode("WORKER")
                    .roleName("运维人员")
                    .dealerId(worker.getDealerId())
                    .permissions(new HashSet<>())
                    .build();

            log.info("[Auth] 微信运维人员登录成功: workerId={}, openId={}", worker.getId(), openId);

            return LoginVO.builder()
                    .accessToken(tokens.accessToken())
                    .refreshToken(tokens.refreshToken())
                    .expiresIn(tokens.accessExpire())
                    .userInfo(userInfo)
                    .build();
        }

        // 3. 查询客户（by openId）
        Customer customer = customerMapper.selectByOpenId(openId);

        // 4. 客户不存在则自动注册为游客
        if (customer == null) {
            customer = new Customer();
            customer.setOpenId(openId);
            customer.setUnionId(miniSession.unionId());
            customer.setName(dto.getNickName() != null && !dto.getNickName().isEmpty()
                    ? dto.getNickName() : "微信用户");
            customer.setCustomerType("INDIVIDUAL");
            customer.setStatus("GUEST");  // 新用户默认游客状态
            customer.setBalance(0L);
            customer.setRemainDuration(0);
            customer.setRemainFlow(0L);
            customer.setRegisteredAt(LocalDateTime.now());
            customerMapper.insert(customer);
            log.info("[Auth] 微信新用户自动注册(游客): openId={}, customerId={}", openId, customer.getId());
        }

        // 5. 状态校验
        String status = customer.getStatus();
        if (status != null && ("DISABLED".equals(status) || "FROZEN".equals(status) || "INACTIVE".equals(status))) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用");
        }

        // 6. 确定用户类型：GUEST 或 CUSTOMER
        String userType = "GUEST".equals(status) ? "GUEST" : "CUSTOMER";
        String roleName = "GUEST".equals(status) ? "游客" : "客户";

        // 7. 构建 CurrentUser
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(customer.getId());
        currentUser.setUserName(customer.getName() != null ? customer.getName() : "微信用户");
        currentUser.setUserType(userType);
        currentUser.setPermissions(new HashSet<>());

        // 8. 生成 JWT
        TokenPair tokens = issueTokens(currentUser);

        // 9. 存入 Redis

        // 10. 构建返回
        UserInfoVO userInfo = UserInfoVO.builder()
                .userId(customer.getId())
                .userName(currentUser.getUserName())
                .userType(userType)
                .roleCode(userType)
                .roleName(roleName)
                .permissions(new HashSet<>())
                .build();

        log.info("[Auth] 微信用户登录成功: customerId={}, openId={}, userType={}", customer.getId(), openId, userType);

        return LoginVO.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresIn(tokens.accessExpire())
                .userInfo(userInfo)
                .build();
    }

    /**
     * 客户手机号密码登录
     */
    private MiniAppSession resolveMiniAppSession(WxLoginDTO dto) {
        if (Boolean.TRUE.equals(miniAppMock)) {
            return resolveMockMiniAppSession(dto);
        }
        return requestWechatMiniAppSession(dto.getCode());
    }

    private MiniAppSession resolveMockMiniAppSession(WxLoginDTO dto) {
        if (StringUtils.hasText(dto.getMockOpenId())) {
            return new MiniAppSession(dto.getMockOpenId().trim(), null);
        }

        String source = StringUtils.hasText(dto.getCode()) ? dto.getCode() : String.valueOf(System.nanoTime());
        String digest = DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
        return new MiniAppSession("mock_mini_" + digest.substring(0, 24), null);
    }

    private MiniAppSession requestWechatMiniAppSession(String code) {
        if (!StringUtils.hasText(miniAppId) || !StringUtils.hasText(miniAppSecret)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "未配置微信小程序 appid/appsecret");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", miniAppId)
                .queryParam("secret", miniAppSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (root.has("errcode") && root.path("errcode").asInt() != 0) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "微信登录失败：" + root.path("errmsg").asText("code2session error"));
            }

            String openId = root.path("openid").asText("");
            if (!StringUtils.hasText(openId)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "微信登录未返回 openid");
            }

            String unionId = root.path("unionid").asText(null);
            return new MiniAppSession(openId, unionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Auth] WeChat miniapp jscode2session failed", e);
            throw new BusinessException(ResultCode.PARAM_INVALID, "微信登录服务暂不可用");
        }
    }

    private record MiniAppSession(String openId, String unionId) {
    }

    @Override
    public LoginVO customerLogin(CustomerLoginDTO dto, String clientIp) {
        // 1. 按手机号查询客户
        Customer customer = customerMapper.selectByPhone(dto.getPhone());
        if (customer == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND, "手机号未注册");
        }

        // 2. 密码校验 (BCrypt)
        if (customer.getPassword() == null || !bCryptPasswordEncoder.matches(dto.getPassword(), customer.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "手机号或密码不正确");
        }

        // 3. 状态校验
        String status = customer.getStatus();
        if (status != null && ("DISABLED".equals(status) || "FROZEN".equals(status) || "INACTIVE".equals(status))) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用");
        }

        // 4. 构建 CurrentUser
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(customer.getId());
        currentUser.setUserName(customer.getName() != null ? customer.getName() : "用户");
        currentUser.setUserType("CUSTOMER");
        currentUser.setPermissions(new HashSet<>());

        // 5. 生成 JWT
        TokenPair tokens = issueTokens(currentUser);

        // 6. 存入 Redis

        // 7. 判断是否首次登录（password_changed=false 表示还没改过初始密码）
        boolean firstLogin = customer.getPasswordChanged() == null || !customer.getPasswordChanged();

        // 8. 构建返回
        UserInfoVO userInfo = UserInfoVO.builder()
                .userId(customer.getId())
                .userName(currentUser.getUserName())
                .userType("CUSTOMER")
                .roleCode("CUSTOMER")
                .roleName("客户")
                .permissions(new HashSet<>())
                .build();

        log.info("[Auth] 客户手机号登录成功: customerId={}, phone={}, firstLogin={}",
                customer.getId(), dto.getPhone(), firstLogin);

        return LoginVO.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresIn(tokens.accessExpire())
                .userInfo(userInfo)
                .firstLogin(firstLogin)
                .build();
    }

    /**
     * 客户修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void customerChangePassword(Long customerId, CustomerChangePasswordDTO dto) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND, "客户不存在");
        }

        // 更新密码
        String encodedPassword = bCryptPasswordEncoder.encode(dto.getNewPassword());
        customer.setPassword(encodedPassword);
        customer.setPasswordChanged(true);
        customerMapper.updateById(customer);

        // 清除 Redis 中的 token，强制重新登录
        deletePossibleIdentityTokens(customerId);

        log.info("[Auth] 客户修改密码成功: customerId={}", customerId);
    }

    /**
     * 运维人员手机号密码登录
     */
    @Override
    public LoginVO workerLogin(CustomerLoginDTO dto, String clientIp) {
        // 1. 按手机号查询运维人员
        Worker worker = workerMapper.selectByPhone(dto.getPhone());
        if (worker == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND, "手机号未注册");
        }

        // 2. 密码校验 (BCrypt)
        if (worker.getPassword() == null || !bCryptPasswordEncoder.matches(dto.getPassword(), worker.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "手机号或密码不正确");
        }

        // 3. 状态校验
        String status = worker.getStatus();
        if (status != null && ("DISABLED".equals(status) || "CANCELLED".equals(status))) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用");
        }

        // 4. 构建 CurrentUser
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(worker.getId());
        currentUser.setUserName(worker.getName() != null ? worker.getName() : "运维人员");
        currentUser.setUserType("WORKER");
        currentUser.setDealerId(worker.getDealerId());
        currentUser.setPermissions(new HashSet<>());

        // 5. 生成 JWT
        TokenPair tokens = issueTokens(currentUser);

        // 6. 存入 Redis

        // 7. 判断是否首次登录
        boolean firstLogin = worker.getPasswordChanged() == null || !worker.getPasswordChanged();

        // 8. 构建返回
        UserInfoVO userInfo = UserInfoVO.builder()
                .userId(worker.getId())
                .userName(currentUser.getUserName())
                .userType("WORKER")
                .roleCode("WORKER")
                .roleName("运维人员")
                .dealerId(worker.getDealerId())
                .permissions(new HashSet<>())
                .build();

        log.info("[Auth] 运维人员手机号登录成功: workerId={}, phone={}, firstLogin={}",
                worker.getId(), dto.getPhone(), firstLogin);

        return LoginVO.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresIn(tokens.accessExpire())
                .userInfo(userInfo)
                .firstLogin(firstLogin)
                .build();
    }

    /**
     * 运维人员修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void workerChangePassword(Long workerId, CustomerChangePasswordDTO dto) {
        Worker worker = workerMapper.selectById(workerId);
        if (worker == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND, "运维人员不存在");
        }

        // 更新密码
        String encodedPassword = bCryptPasswordEncoder.encode(dto.getNewPassword());
        worker.setPassword(encodedPassword);
        worker.setPasswordChanged(true);
        workerMapper.updateById(worker);

        // 清除 Redis 中的 token，强制重新登录
        deletePossibleIdentityTokens(workerId);

        log.info("[Auth] 运维人员修改密码成功: workerId={}", workerId);
    }
}
