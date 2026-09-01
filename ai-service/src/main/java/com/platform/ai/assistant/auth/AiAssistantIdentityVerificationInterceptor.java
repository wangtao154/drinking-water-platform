package com.platform.ai.assistant.auth;

import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * Fails closed unless the calling backend account has a valid manual identity
 * verification record. This check is intentionally isolated to AI endpoints.
 */
@Component
@RequiredArgsConstructor
public class AiAssistantIdentityVerificationInterceptor implements HandlerInterceptor {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        CurrentUser user = UserContext.get();
        if (user == null || user.getUserId() == null) {
            return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 40100, "未登录或登录已过期");
        }
        try {
            Integer matched = jdbcTemplate.queryForObject("""
                            SELECT COUNT(1)
                            FROM sys_user
                            WHERE id = ?
                              AND deleted = 0
                              AND status = 'ENABLED'
                              AND identity_verified = 1
                              AND name IS NOT NULL AND TRIM(name) <> ''
                              AND phone REGEXP '^1[3-9][0-9]{9}$'
                            """, Integer.class, user.getUserId());
            if (matched != null && matched > 0) {
                return true;
            }
            return writeError(response, HttpServletResponse.SC_FORBIDDEN, 40302,
                    "当前后台账户尚未完成身份核验，请联系系统管理员核验真实姓名和手机号后再使用AI助手");
        } catch (DataAccessException ex) {
            return writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, 50301,
                    "AI身份核验服务暂不可用，请稍后重试");
        }
    }

    private static boolean writeError(HttpServletResponse response, int httpStatus, int code, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message
                + "\",\"data\":null,\"timestamp\":\"" + LocalDateTime.now() + "\"}");
        return false;
    }
}
