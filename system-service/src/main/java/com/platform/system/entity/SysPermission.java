package com.platform.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private Long parentId;

    private String path;

    private String icon;

    private Integer sortOrder;

    private String status;
}
