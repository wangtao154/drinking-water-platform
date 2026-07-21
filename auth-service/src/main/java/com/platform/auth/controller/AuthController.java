package com.platform.auth.controller;

import com.platform.auth.dto.ChangePasswordDTO;
import com.platform.auth.dto.CustomerChangePasswordDTO;
import com.platform.auth.dto.CustomerLoginDTO;
import com.platform.auth.dto.LoginDTO;
import com.platform.auth.dto.RefreshTokenDTO;
import com.platform.auth.dto.WxLoginDTO;
import com.platform.auth.service.AuthService;
import com.platform.auth.vo.LoginVO;
import com.platform.auth.vo.UserInfoVO;
import com.platform.common.auth.UserContext;
import com.platform.common.base.BaseController;
import com.platform.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证授权 API
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    /**
     * 账号密码登录
     */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return success(authService.login(dto, clientIp));
    }

    /**
     * 微信小程序登录（Mock 模式）
     */
    @PostMapping("/wx-login")
    public R<LoginVO> wxLogin(@Valid @RequestBody WxLoginDTO dto, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return success(authService.wxLogin(dto, clientIp));
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return success(authService.refreshToken(dto.getRefreshToken()));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = UserContext.getUserId();
        String accessToken = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : null;
        authService.logout(userId, accessToken);
        return success();
    }

    /**
     * 获取当前登录用户信息及权限
     */
    @GetMapping("/current-user")
    public R<UserInfoVO> currentUser() {
        Long userId = UserContext.getUserId();
        return success(authService.getCurrentUserInfo(userId));
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = UserContext.getUserId();
        authService.changePassword(userId, dto);
        return success();
    }

    /**
     * 客户手机号密码登录
     */
    @PostMapping("/customer-login")
    public R<LoginVO> customerLogin(@Valid @RequestBody CustomerLoginDTO dto, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return success(authService.customerLogin(dto, clientIp));
    }

    /**
     * 客户修改密码（首次登录强制修改 / 日常修改）
     */
    @PostMapping("/customer-change-password")
    public R<Void> customerChangePassword(@Valid @RequestBody CustomerChangePasswordDTO dto) {
        Long customerId = UserContext.getUserId();
        authService.customerChangePassword(customerId, dto);
        return success();
    }

    /**
     * 运维人员手机号密码登录
     */
    @PostMapping("/worker-login")
    public R<LoginVO> workerLogin(@Valid @RequestBody CustomerLoginDTO dto, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return success(authService.workerLogin(dto, clientIp));
    }

    /**
     * 运维人员修改密码（首次登录强制修改 / 日常修改）
     */
    @PostMapping("/worker-change-password")
    public R<Void> workerChangePassword(@Valid @RequestBody CustomerChangePasswordDTO dto) {
        Long workerId = UserContext.getUserId();
        authService.workerChangePassword(workerId, dto);
        return success();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 取第一个 IP（多层代理时）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
