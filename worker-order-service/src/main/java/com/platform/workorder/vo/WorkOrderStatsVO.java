package com.platform.workorder.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工单统计 VO
 */
@Data
public class WorkOrderStatsVO implements Serializable {

    /**
     * 总数
     */
    private Long total;

    /**
     * 待处理数
     */
    private Long pending;

    /**
     * 已派单数
     */
    private Long dispatched;

    /**
     * 已接单数
     */
    private Long accepted;

    /**
     * 进行中数
     */
    private Long inProgress;

    /**
     * 已完成数（含已完成+已核实）
     */
    private Long completed;

    /**
     * 已取消数
     */
    private Long cancelled;

    /**
     * 平均完成耗时（小时）
     */
    private BigDecimal avgCompleteHours;
}
