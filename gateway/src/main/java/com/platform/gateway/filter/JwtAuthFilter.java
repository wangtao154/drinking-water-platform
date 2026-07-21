package com.platform.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * JWT 全局认证过滤器
 * 1. 白名单路径直接放行
 * 2. 解析 Authorization header 中的 Bearer Token
 * 3. 验证 token 有效性
 * 4. 注入 X-User-Id / X-User-Name / X-User-Type 等下游 header
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:drinking-water-platform-jwt-secret-key-please-change-in-production-2026}")
    private String secret;

    private SecretKey key;

    /** 白名单路径（不需要认证） */
    private static final Set<String> WHITELIST_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/wx-login",
            "/api/v1/auth/customer-login",
            "/api/v1/auth/worker-login",
            "/api/v1/auth/wechat-login",
            "/api/v1/auth/sms-login",
            "/api/v1/wechat/official/callback",
            "/actuator",
            "/actuator/health",
            "/actuator/info"
    );

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单检查
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取 Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, 40100, "未登录或Token已过期");
        }

        String token = authHeader.substring(7);

        // 3. 解析 JWT
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 4. 注入下游 header（userName URL 编码，避免 HTTP 头中文乱码）
            String userName = claims.get("userName", String.class);
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-User-Name", userName != null ? URLEncoder.encode(userName, StandardCharsets.UTF_8) : "")
                    .header("X-User-Type", claims.get("userType", String.class) != null ? claims.get("userType", String.class) : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("[Gateway] JWT验证失败: {}", e.getMessage());
            return unauthorized(exchange, 40104, "Token无效或已过期");
        }
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null,\"timestamp\":\"" +
                java.time.LocalDateTime.now() + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 优先级：在路由之前执行
        return -100;
    }
}
