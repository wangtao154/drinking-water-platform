package com.platform.report.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘统计 VO
 */
@Data
public class DashboardVO implements Serializable {

    /**
     * 设备总数
     */
    private Long totalDevices;

    /**
     * 在线设备数
     */
    private Long onlineDevices;

    /**
     * 离线设备数
     */
    private Long offlineDevices;

    /**
     * 客户总数
     */
    private Long totalCustomers;

    /**
     * 订单总数
     */
    private Long totalOrders;

    /**
     * 总收入（分）
     */
    private Long totalRevenue;

    /**
     * 待处理订单数
     */
    private Long pendingOrders;

    /**
     * 滤芯总数
     */
    private Long totalFilters;

    /**
     * 维修工总数
     */
    private Long totalWorkers;

    /**
     * 经销商总数
     */
    private Long totalDealers;

    /**
     * 活跃告警数
     */
    private Long activeAlerts;
}
