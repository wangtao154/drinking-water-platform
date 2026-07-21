package com.platform.pkg.interceptor;

import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.JwtUtil;
import com.platform.common.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证拦截器
 * 从 Authorization header 或 Gateway header 解析用户信息，设置 UserContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 优先检查 Gateway 转发的 header（生产环境由 Gateway 注入）
        String headerUserId = request.getHeader("X-User-Id");
        if (headerUserId != null && !headerUserId.isEmpty()) {
            CurrentUser currentUser = new CurrentUser();
            currentUser.setUserId(Long.parseLong(headerUserId));
            // URL 解码用户名（Gateway 编码后传递）
            String headerUserName = request.getHeader("X-User-Name");
            if (headerUserName != null && !headerUserName.isEmpty()) {
                currentUser.setUserName(URLDecoder.decode(headerUserName, StandardCharsets.UTF_8));
            }
            currentUser.setUserType(request.getHeader("X-User-Type"));
            UserContext.set(currentUser);
            return true;
        }

        // 2. 直接解析 JWT（开发/测试环境无 Gateway 时）
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40100,\"message\":\"未登录或token已失效\",\"data\":null,\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}");
            return false;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40100,\"message\":\"token无效或已过期\",\"data\":null,\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}");
            return false;
        }

        // 3. 设置 UserContext
        CurrentUser currentUser = jwtUtil.parseToCurrentUser(token);
        if (currentUser == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40100,\"message\":\"token解析失败\",\"data\":null,\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}");
            return false;
        }

        UserContext.set(currentUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
