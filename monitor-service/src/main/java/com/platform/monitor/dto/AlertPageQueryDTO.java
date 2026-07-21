package com.platform.monitor.dto;

import com.platform.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警分页查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertPageQueryDTO extends PageQueryDTO {

    /**
     * 设备ID（可选）
     */
    private String deviceId;

    /**
     * 告警类型（可选）
     */
    private String alertType;

    /**
     * 告警级别（可选）：WARNING / ALARM
     */
    private String alertLevel;

    /**
     * 处理状态（可选）：UNHANDLED / HANDLED
     */
    private String handledStatus;

    /**
     * 推送状态（可选）：UNPUSHED / PUSHED
     */
    private String pushStatus;
}
