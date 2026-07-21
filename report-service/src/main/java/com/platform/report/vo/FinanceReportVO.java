package com.platform.report.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 财务统计 VO
 */
@Data
public class FinanceReportVO implements Serializable {

    /**
     * 佣金总额（分）
     */
    private Long totalCommission;

    /**
     * 已结算佣金（分）
     */
    private Long settledCommission;

    /**
     * 待结算佣金（分）
     */
    private Long pendingCommission;

    /**
     * 消费总额（分）
     */
    private Long totalConsumption;

    /**
     * 总收入（分）
     */
    private Long totalRevenue;

    /**
     * 待结算账单数
     */
    private Long pendingSettlements;
}
