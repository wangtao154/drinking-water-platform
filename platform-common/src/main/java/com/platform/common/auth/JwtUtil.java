package com.platform.common.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具类
 * 签发与校验 Access Token / Refresh Token
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:drinking-water-platform-jwt-secret-key-default}")
    private String secret;

    @Value("${jwt.access-expire:7200}")
    private Long accessExpireSeconds;

    @Value("${jwt.refresh-expire:604800}")
    private Long refreshExpireSeconds;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ==================== 签发 ====================

    /**
     * 签发 Access Token（便捷方法，基于 CurrentUser）
     */
    public String createAccessToken(CurrentUser currentUser) {
        List<String> permList = currentUser.getPermissions() != null
                ? List.copyOf(currentUser.getPermissions()) : null;
        return buildToken(currentUser.getUserId(), currentUser.getUserName(), currentUser.getUserType(),
                permList, null, accessExpireSeconds, "ACCESS");
    }

    public String createAccessToken(CurrentUser currentUser, String sessionId) {
        List<String> permList = currentUser.getPermissions() != null
                ? List.copyOf(currentUser.getPermissions()) : null;
        return buildToken(currentUser.getUserId(), currentUser.getUserName(), currentUser.getUserType(),
                permList, sessionId, accessExpireSeconds, "ACCESS");
    }

    /**
     * 签发 Access Token
     */
    public String createAccessToken(Long userId, String userType, List<String> permissions) {
        return buildToken(userId, null, userType, permissions, null, accessExpireSeconds, "ACCESS");
    }

    /**
     * 签发 Refresh Token（便捷方法，基于 CurrentUser）
     */
    public String createRefreshToken(CurrentUser currentUser) {
        return buildToken(currentUser.getUserId(), currentUser.getUserName(), currentUser.getUserType(),
                null, null, refreshExpireSeconds, "REFRESH");
    }

    public String createRefreshToken(CurrentUser currentUser, String sessionId) {
        return buildToken(currentUser.getUserId(), currentUser.getUserName(), currentUser.getUserType(),
                null, sessionId, refreshExpireSeconds, "REFRESH");
    }

    /**
     * 签发 Refresh Token
     */
    public String createRefreshToken(Long userId, String userType) {
        return buildToken(userId, null, userType, null, null, refreshExpireSeconds, "REFRESH");
    }

    private String buildToken(Long userId, String userName, String userType, List<String> permissions,
                              String sessionId, Long expireSeconds, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireSeconds * 1000);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("userName", userName != null ? userName : "")
                .claim("userType", userType)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);

        if (permissions != null && !permissions.isEmpty()) {
            builder.claim("permissions", permissions);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sessionId", sessionId);
        }

        return builder.compact();
    }

    // ==================== 解析 ====================

    /**
     * 解析 Token
     *
     * @return Claims 或 null（解析失败）
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.warn("[JWT] Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 校验 ====================

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 从 Token 提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从 Token 提取用户类型
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userType", String.class) : null;
    }

    public String getSessionIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("sessionId", String.class) : null;
    }

    /**
     * 从 Token 提取权限列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return List.of();
        Object perms = claims.get("permissions");
        if (perms instanceof List) {
            return (List<String>) perms;
        }
        return List.of();
    }

    /**
     * 从 Token 提取全部用户信息
     */
    public Map<String, Object> getAllClaims(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims : Map.of();
    }

    /**
     * 解析 Token 为 CurrentUser
     */
    @SuppressWarnings("unchecked")
    public CurrentUser parseToCurrentUser(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;

        CurrentUser currentUser = new CurrentUser();
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            currentUser.setUserId(((Integer) userIdObj).longValue());
        } else if (userIdObj instanceof Long) {
            currentUser.setUserId((Long) userIdObj);
        }
        currentUser.setUserName(claims.get("userName", String.class));
        currentUser.setUserType(claims.get("userType", String.class));

        Object perms = claims.get("permissions");
        if (perms instanceof List) {
            currentUser.setPermissions(new java.util.HashSet<>((List<String>) perms));
        }
        return currentUser;
    }

    // ==================== 过期时间 ====================

    public Long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public Long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    /** Access Token 过期时间（秒），便捷别名 */
    public Long getAccessTokenExpire() {
        return accessExpireSeconds;
    }

    /** Refresh Token 过期时间（秒），便捷别名 */
    public Long getRefreshTokenExpire() {
        return refreshExpireSeconds;
    }
}
