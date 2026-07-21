package com.platform.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应体
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 当前页数据
     */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Long total, Integer size, Long current, List<T> records) {
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = size > 0 ? (total + size - 1) / size : 0L;
        this.records = records;
    }

    /**
     * 从 MyBatis-Plus IPage 构造
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(
                page.getTotal(),
                (int) page.getSize(),
                page.getCurrent(),
                page.getRecords()
        );
    }

    /**
     * 空结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 20, 1L, 0L, Collections.emptyList());
    }

    public PageResult(Long total, Integer size, Long current, Long pages, List<T> records) {
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = pages;
        this.records = records;
    }
}
