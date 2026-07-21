package com.platform.report.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修工统计 VO
 */
@Data
public class WorkerReportVO implements Serializable {

    /**
     * 维修工总数
     */
    private Long totalWorkers;

    /**
     * 活跃维修工数
     */
    private Long activeWorkers;

    /**
     * 工单总数
     */
    private Long totalWorkOrders;

    /**
     * 待处理工单数
     */
    private Long pendingWorkOrders;

    /**
     * 已完成工单数
     */
    private Long completedWorkOrders;

    /**
     * 平均评分
     */
    private Long avgRating;
}
