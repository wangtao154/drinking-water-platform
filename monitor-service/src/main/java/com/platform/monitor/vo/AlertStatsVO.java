package com.platform.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警统计 VO
 */
@Data
public class AlertStatsVO implements Serializable {

    /**
     * 总告警数
     */
    private Long totalAlerts;

    /**
     * 未处理告警数
     */
    private Long unhandledAlerts;

    /**
     * 已处理告警数
     */
    private Long handledAlerts;

    /**
     * 预警级告警数
     */
    private Long warningAlerts;

    /**
     * 报警级告警数
     */
    private Long alarmAlerts;

    /**
     * 已推送告警数
     */
    private Long pushedAlerts;
}
