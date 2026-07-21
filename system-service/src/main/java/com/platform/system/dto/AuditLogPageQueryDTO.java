package com.platform.system.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审计日志分页查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogPageQueryDTO extends PageQueryDTO {

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 结果
     */
    private String result;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}
