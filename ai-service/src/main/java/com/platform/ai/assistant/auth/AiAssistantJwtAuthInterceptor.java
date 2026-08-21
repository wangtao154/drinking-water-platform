package com.platform.ai.assistant.auth;

import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.JwtUtil;
import com.platform.common.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Authenticates only the admin assistant endpoints. Gateway identity headers
 * are preferred; a JWT fallback is available for trusted local calls.
 */
@Component
@RequiredArgsConstructor
public class AiAssistantJwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                CurrentUser currentUser = new CurrentUser();
                currentUser.setUserId(Long.parseLong(userIdHeader));
                String userName = request.getHeader("X-User-Name");
                currentUser.setUserName(userName == null || userName.isBlank()
                        ? null
                        : URLDecoder.decode(userName, StandardCharsets.UTF_8));
                currentUser.setUserType(request.getHeader("X-User-Type"));
                currentUser.setPermissions(parsePermissions(request.getHeader("X-User-Permissions")));
                UserContext.set(currentUser);
                return true;
            } catch (IllegalArgumentException ex) {
                return writeUnauthorized(response, "登录身份信息无效");
            }
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            CurrentUser currentUser = jwtUtil.parseToCurrentUser(authorization.substring(7));
            if (currentUser != null && currentUser.getUserId() != null) {
                UserContext.set(currentUser);
                return true;
            }
        }

        return writeUnauthorized(response, "未登录或登录已过期");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private static HashSet<String> parsePermissions(String value) {
        HashSet<String> permissions = new HashSet<>();
        if (value == null || value.isBlank()) {
            return permissions;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(permission -> !permission.isEmpty())
                .forEach(permissions::add);
        return permissions;
    }

    private static boolean writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":40100,\"message\":\"" + message
                + "\",\"data\":null,\"timestamp\":\"" + LocalDateTime.now() + "\"}");
        return false;
    }
}
