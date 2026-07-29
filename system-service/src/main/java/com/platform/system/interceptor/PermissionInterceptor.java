package com.platform.system.interceptor;

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
 * RBAC permission interceptor for system-service controller methods.
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (annotation == null || annotation.value().length == 0) {
            return true;
        }

        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            writeError(response, 401, 40100, "未登录或Token已过期");
            return false;
        }

        boolean allowed = annotation.requireAll()
                ? Arrays.stream(annotation.value()).allMatch(currentUser::hasPermission)
                : Arrays.stream(annotation.value()).anyMatch(currentUser::hasPermission);

        if (!allowed) {
            writeError(response, 403, 40300, "无权限访问");
            return false;
        }

        return true;
    }

    private void writeError(HttpServletResponse response, int status, int code, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code
                + ",\"message\":\"" + message
                + "\",\"data\":null,\"timestamp\":\"" + LocalDateTime.now() + "\"}");
    }
}
