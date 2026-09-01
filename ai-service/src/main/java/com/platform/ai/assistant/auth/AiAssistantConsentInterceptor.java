package com.platform.ai.assistant.auth;

import com.platform.common.ai.AiAssistantPolicy;
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
 * Fails closed until the current backend account has explicitly accepted the
 * current platform user agreement and privacy policy.
 */
@Component
@RequiredArgsConstructor
public class AiAssistantConsentInterceptor implements HandlerInterceptor {

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
                            FROM ai_assistant_consent_log
                            WHERE account_id = ?
                              AND consent_type = ?
                              AND policy_version = ?
                              AND accepted = 1
                            """, Integer.class, user.getUserId(), AiAssistantPolicy.CONSENT_TYPE, AiAssistantPolicy.VERSION);
            if (matched != null && matched > 0) {
                return true;
            }
            return writeError(response, HttpServletResponse.SC_FORBIDDEN, 40303,
                    "使用 AI 助手前请先阅读并同意《用户协议》和《隐私政策》");
        } catch (DataAccessException ex) {
            return writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, 50302,
                    "用户协议与隐私政策确认服务暂不可用，请稍后重试");
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
