package com.platform.common.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Feign 请求拦截器
 * 在微服务间调用时自动传递用户上下文信息（通过 HTTP Header）
 * userName URL 编码后传递，避免 HTTP 头中文乱码
 */
@Component
public class TokenUserInterceptor implements RequestInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_DEALER_ID = "X-Dealer-Id";

    @Override
    public void apply(RequestTemplate template) {
        CurrentUser user = UserContext.get();
        if (user == null) return;

        if (user.getUserId() != null) {
            template.header(HEADER_USER_ID, String.valueOf(user.getUserId()));
        }
        if (user.getUserName() != null) {
            template.header(HEADER_USER_NAME, URLEncoder.encode(user.getUserName(), StandardCharsets.UTF_8));
        }
        if (user.getUserType() != null) {
            template.header(HEADER_USER_TYPE, user.getUserType());
        }
        if (user.getDealerId() != null) {
            template.header(HEADER_DEALER_ID, String.valueOf(user.getDealerId()));
        }
    }

    /**
     * 从 HTTP Header 构建当前用户（下游服务接收 Feign 调用时使用）
     */
    public static CurrentUser fromHeaders(String userId, String userName, String userType, String dealerId) {
        if (userId == null) return null;

        return CurrentUser.builder()
                .userId(Long.valueOf(userId))
                .userName(userName)
                .userType(userType)
                .dealerId(dealerId != null ? Long.valueOf(dealerId) : null)
                .build();
    }
}
