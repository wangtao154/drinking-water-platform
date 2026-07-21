package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /** 权限编码（唯一） */
    private String permissionCode;

    /** 权限名称 */
    private String permissionName;

    /** 权限类型：MENU / API / BUTTON */
    private String permissionType;

    /** 父级 ID，null 表示顶级 */
    private Long parentId;

    /** 前端路由路径 */
    private String path;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：ENABLED / DISABLED */
    private String status;
}
