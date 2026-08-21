package com.platform.ai.assistant.auth;

import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.RequirePermission;
import com.platform.common.auth.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Applies @RequirePermission to the isolated assistant API surface.
 */
@Component
public class AiAssistantPermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission required = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (required == null || required.value().length == 0) {
            return true;
        }

        CurrentUser user = UserContext.get();
        boolean allowed = user != null && (required.requireAll()
                ? Arrays.stream(required.value()).allMatch(user::hasPermission)
                : Arrays.stream(required.value()).anyMatch(user::hasPermission));
        if (allowed) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":40300,\"message\":\"无权限访问\",\"data\":null,\"timestamp\":\""
                + LocalDateTime.now() + "\"}");
        return false;
    }
}
