package com.platform.order.vo;

import lombok.Data;

@Data
public class OrderStatsVO {
    private Long totalOrders;
    private Long pendingOrders;
    private Long paidOrders;
    private Long cancelledOrders;
    private Long refundingOrders;
    private Long refundedOrders;
    private Long totalPaidAmount;
    private Long totalRefundAmount;
}
