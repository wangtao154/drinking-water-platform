package com.platform.system.vo;

import com.platform.system.entity.SysRole;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SysRoleVO implements Serializable {

    private Long id;
    private String roleCode;
    private String roleName;
    private String roleDesc;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SysRoleVO fromEntity(SysRole e) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(e.getId());
        vo.setRoleCode(e.getRoleCode());
        vo.setRoleName(e.getRoleName());
        vo.setRoleDesc(e.getRoleDesc());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }
}
