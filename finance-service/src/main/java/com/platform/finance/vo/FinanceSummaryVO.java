package com.platform.finance.vo;

import lombok.Data;

@Data
public class FinanceSummaryVO {
    private Long totalCommission;
    private Long settledCommission;
    private Long pendingCommission;
    private Long totalConsumption;
    private Long totalRevenue;
    private Long pendingSettlements;
    private Long totalInvoices;
    private Long pendingInvoices;
}
