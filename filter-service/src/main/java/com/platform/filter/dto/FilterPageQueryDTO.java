package com.platform.filter.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class FilterPageQueryDTO extends PageQueryDTO {

    private Long modelId;

    private String lifecycleStatus;

    private String currentDeviceId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAtStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAtEnd;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime installedAtStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime installedAtEnd;

    /**
     * 关键词搜索（型号编码/名称模糊匹配）
     */
    private String keyword;

    /**
     * 滤芯型号分类
     */
    private String category;

    /**
     * 滤芯型号状态：ENABLED / DISABLED
     */
    private String status;
}
