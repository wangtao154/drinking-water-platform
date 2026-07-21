package com.platform.monitor.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警处理 DTO
 */
@Data
public class AlertHandleDTO implements Serializable {

    /**
     * 处理状态：HANDLED
     */
    private String handledStatus;

    /**
     * 处理备注
     */
    private String handleRemark;
}
