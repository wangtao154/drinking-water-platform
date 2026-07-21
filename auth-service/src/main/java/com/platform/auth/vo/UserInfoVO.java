package com.platform.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserInfoVO {

    private Long userId;

    private String userName;

    /** 用户类型：SUPER_ADMIN / ADMIN / DEALER / WORKER / CUSTOMER / GUEST */
    private String userType;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 经销商 ID（经销商用户才有） */
    private Long dealerId;

    /** 经销商层级（经销商用户才有） */
    private String dealerLevel;

    /** 权限编码集合 */
    private Set<String> permissions;
}
