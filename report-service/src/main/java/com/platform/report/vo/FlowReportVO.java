package com.platform.report.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 流量统计报表 VO
 */
@Data
public class FlowReportVO implements Serializable {

    /** 总设备数 */
    private Long totalDevices;

    /** 在线设备数 */
    private Long onlineDevices;

    /** 离线设备数 */
    private Long offlineDevices;

    /** 今日上报设备数 */
    private Long todayReportDevices;

    /** 总用水量(升) */
    private Long totalWaterFlow;

    /** 今日用水量(升) */
    private Long todayWaterFlow;

    /** 总净水量(升) */
    private Long totalPureFlow;

    /** 今日净水量(升) */
    private Long todayPureFlow;

    /** 近7天用水量趋势 [{date: "2026-07-03", value: 1200}, ...] */
    private List<Map<String, Object>> dailyFlowTrend;

    /** 设备类型分布 [{name: "RO-500G", value: 30}, ...] */
    private List<Map<String, Object>> modelDistribution;

    /** 在线率(%) */
    private Double onlineRate;
}
