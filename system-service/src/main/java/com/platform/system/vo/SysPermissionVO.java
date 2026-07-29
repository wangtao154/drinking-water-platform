package com.platform.system.vo;

import com.platform.system.entity.SysPermission;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SysPermissionVO {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Long parentId;
    private String path;
    private String icon;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SysPermissionVO> children = new ArrayList<>();

    public static SysPermissionVO fromEntity(SysPermission entity) {
        SysPermissionVO vo = new SysPermissionVO();
        vo.setId(entity.getId());
        vo.setPermissionCode(entity.getPermissionCode());
        vo.setPermissionName(entity.getPermissionName());
        vo.setPermissionType(entity.getPermissionType());
        vo.setParentId(entity.getParentId());
        vo.setPath(entity.getPath());
        vo.setIcon(entity.getIcon());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
