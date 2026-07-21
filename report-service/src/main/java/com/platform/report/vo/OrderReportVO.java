package com.platform.report.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单统计 VO
 */
@Data
public class OrderReportVO implements Serializable {

    /**
     * 订单总数
     */
    private Long totalOrders;

    /**
     * 待支付订单数
     */
    private Long pendingOrders;

    /**
     * 已支付订单数
     */
    private Long paidOrders;

    /**
     * 已取消订单数
     */
    private Long cancelledOrders;

    /**
     * 已退款订单数
     */
    private Long refundedOrders;

    /**
     * 总收入（分）
     */
    private Long totalRevenue;

    /**
     * 今日订单数
     */
    private Long todayOrders;

    /**
     * 今日收入（分）
     */
    private Long todayRevenue;
}
