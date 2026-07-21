package com.platform.finance.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.finance.dto.CommissionPageQueryDTO;
import com.platform.finance.dto.InvoiceCreateDTO;
import com.platform.finance.dto.SettlementCreateDTO;
import com.platform.finance.dto.SettlementPageQueryDTO;
import com.platform.finance.service.FinanceService;
import com.platform.finance.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    // ==================== 分润记录 ====================

    @GetMapping("/commissions")
    public R<PageResult<CommissionRecordVO>> commissionPage(CommissionPageQueryDTO query) {
        return R.ok(financeService.commissionPage(query));
    }

    @PutMapping("/commissions/{orderNo}/settle")
    public R<Void> settleCommission(@PathVariable String orderNo) {
        financeService.settleCommission(orderNo);
        return R.ok();
    }

    // ==================== 结算账单 ====================

    @PostMapping("/settlements")
    public R<SettlementBillVO> createSettlement(@Valid @RequestBody SettlementCreateDTO dto) {
        return R.ok(financeService.createSettlement(dto));
    }

    @GetMapping("/settlements")
    public R<PageResult<SettlementBillVO>> settlementPage(SettlementPageQueryDTO query) {
        return R.ok(financeService.settlementPage(query));
    }

    @PutMapping("/settlements/{billNo}/settle")
    public R<Void> settleBill(@PathVariable String billNo) {
        financeService.settleBill(billNo);
        return R.ok();
    }

    // ==================== 发票管理 ====================

    @PostMapping("/invoices")
    public R<InvoiceVO> createInvoice(@Valid @RequestBody InvoiceCreateDTO dto) {
        return R.ok(financeService.createInvoice(dto));
    }

    @GetMapping("/invoices")
    public R<PageResult<InvoiceVO>> invoicePage(com.platform.common.dto.PageQueryDTO query) {
        return R.ok(financeService.invoicePage(query));
    }

    @GetMapping("/invoices/{invoiceNo}")
    public R<InvoiceVO> getInvoiceByNo(@PathVariable String invoiceNo) {
        return R.ok(financeService.getInvoiceByNo(invoiceNo));
    }

    @PutMapping("/invoices/{invoiceNo}/issue")
    public R<Void> issueInvoice(@PathVariable String invoiceNo) {
        financeService.issueInvoice(invoiceNo);
        return R.ok();
    }

    // ==================== 财务汇总 ====================

    @GetMapping("/summary")
    public R<FinanceSummaryVO> summary() {
        return R.ok(financeService.getSummary());
    }
}
