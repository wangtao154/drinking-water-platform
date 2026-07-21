package com.platform.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求 DTO
 * 所有分页查询接口的父类
 */
@Data
public class PageQueryDTO implements Serializable {

    /**
     * 页码（默认 1）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小（默认 20，最大 100）
     */
    private Integer pageSize = 20;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 排序方向：ASC / DESC
     */
    private String orderDirection = "DESC";

    /**
     * 校正分页参数
     */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (orderDirection != null && !"ASC".equalsIgnoreCase(orderDirection) && !"DESC".equalsIgnoreCase(orderDirection)) {
            orderDirection = "DESC";
        }
    }
}
