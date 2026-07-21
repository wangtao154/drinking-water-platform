package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 工号（唯一） */
    private String employeeNo;

    /** 登录账号（唯一） */
    private String username;

    /** BCrypt 加密密码 */
    private String password;

    /** 关联角色 ID */
    private Long roleId;

    /** 真实姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 微信号 */
    private String wechat;

    /** 部门 */
    private String department;

    /** 最后登录 IP */
    private String lastLoginIp;

    /** 最后登录时间 */
    private java.time.LocalDateTime lastLoginAt;

    /** 状态：ENABLED / DISABLED */
    private String status;
}
