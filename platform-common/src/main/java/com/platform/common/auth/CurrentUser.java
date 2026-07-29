package com.platform.common.auth;

import lombok.*;

import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户信息
 * 存储在 ThreadLocal 中，供全链路使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名/姓名
     */
    private String userName;

    /**
     * 用户类型：ADMIN / DEALER / WORKER / CUSTOMER
     */
    private String userType;

    /**
     * 经销商ID（仅 DEALER 类型有值）
     */
    private Long dealerId;

    /**
     * 经销商层级（仅 DEALER 类型有值）：L1_DEALER / L2_DEALER / L3_DEALER
     */
    private String dealerLevel;

    /**
     * 权限编码集合
     */
    private Set<String> permissions;

    /**
     * 判断是否拥有某权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && (permissions.contains("*") || permissions.contains(permission));
    }

    /**
     * 判断是否为超级管理员
     */
    public boolean isAdmin() {
        return "SUPER_ADMIN".equals(userType) || "ADMIN".equals(userType);
    }

    /**
     * 判断是否为经销商
     */
    public boolean isDealer() {
        return "DEALER".equals(userType);
    }

    /**
     * 判断是否为运维人员
     */
    public boolean isWorker() {
        return "WORKER".equals(userType);
    }

    /**
     * 判断是否为客户
     */
    public boolean isCustomer() {
        return "CUSTOMER".equals(userType);
    }
}
