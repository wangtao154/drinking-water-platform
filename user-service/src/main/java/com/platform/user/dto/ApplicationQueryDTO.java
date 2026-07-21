package com.platform.user.dto;

import lombok.Data;

/**
 * 申请列表查询 DTO（PC端使用）
 */
@Data
public class ApplicationQueryDTO {

    /** 申请类型筛选 */
    private String applyType;

    /** 状态筛选 */
    private String status;

    /** 关键词（姓名/手机号） */
    private String keyword;

    private Integer page = 1;
    private Integer size = 10;

    public void normalize() {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        if (size > 100) size = 100;
    }
}
