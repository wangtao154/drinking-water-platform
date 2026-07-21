package com.platform.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String employeeNo;
    private String username;
    private String password;
    private Long roleId;
    private String name;
    private String phone;
    private String email;
    private String wechat;
    private String department;
    private String lastLoginIp;
    private LocalDateTime lastLoginAt;
    private String status;
}
