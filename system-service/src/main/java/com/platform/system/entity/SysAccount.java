package com.platform.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统账户实体 (sys_user 表)
 */
@Data
@TableName("sys_user")
public class SysAccount implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    /**
     * Whether an administrator has verified this backend account using the
     * account holder's name and bound mobile number.
     */
    private Boolean identityVerified;

    private LocalDateTime identityVerifiedAt;

    private Long identityVerifiedBy;

    private String identityVerificationMethod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
