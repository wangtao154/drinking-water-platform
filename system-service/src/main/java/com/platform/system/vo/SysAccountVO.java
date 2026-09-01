package com.platform.system.vo;

import com.platform.system.entity.SysAccount;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SysAccountVO implements Serializable {

    private Long id;
    private String employeeNo;
    private String username;
    private Long roleId;
    private String roleName;
    private String name;
    private String phone;
    private String email;
    private String wechat;
    private String department;
    private String lastLoginIp;
    private LocalDateTime lastLoginAt;
    private String status;
    private Boolean identityVerified;
    private LocalDateTime identityVerifiedAt;
    private Long identityVerifiedBy;
    private String identityVerificationMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SysAccountVO fromEntity(SysAccount e) {
        SysAccountVO vo = new SysAccountVO();
        vo.setId(e.getId());
        vo.setEmployeeNo(e.getEmployeeNo());
        vo.setUsername(e.getUsername());
        vo.setRoleId(e.getRoleId());
        vo.setName(e.getName());
        vo.setPhone(e.getPhone());
        vo.setEmail(e.getEmail());
        vo.setWechat(e.getWechat());
        vo.setDepartment(e.getDepartment());
        vo.setLastLoginIp(e.getLastLoginIp());
        vo.setLastLoginAt(e.getLastLoginAt());
        vo.setStatus(e.getStatus());
        vo.setIdentityVerified(Boolean.TRUE.equals(e.getIdentityVerified()));
        vo.setIdentityVerifiedAt(e.getIdentityVerifiedAt());
        vo.setIdentityVerifiedBy(e.getIdentityVerifiedBy());
        vo.setIdentityVerificationMethod(e.getIdentityVerificationMethod());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }
}
