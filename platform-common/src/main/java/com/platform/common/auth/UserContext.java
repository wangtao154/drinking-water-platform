package com.platform.common.auth;

/**
 * ThreadLocal 上下文工具
 * 在拦截器中设置当前用户，在 Service / Controller 中获取
 */
public class UserContext {

    private static final ThreadLocal<CurrentUser> CONTEXT = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        CONTEXT.set(user);
    }

    public static CurrentUser get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        CurrentUser user = get();
        return user != null ? user.getUserId() : null;
    }

    public static String getUserType() {
        CurrentUser user = get();
        return user != null ? user.getUserType() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
