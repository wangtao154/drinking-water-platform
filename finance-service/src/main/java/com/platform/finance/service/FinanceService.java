package com.platform.finance.service;

import com.platform.common.dto.PageQueryDTO;
import com.platform.common.result.PageResult;
import com.platform.finance.dto.CommissionPageQueryDTO;
import com.platform.finance.dto.InvoiceCreateDTO;
import com.platform.finance.dto.SettlementCreateDTO;
import com.platform.finance.dto.SettlementPageQueryDTO;
import com.platform.finance.vo.*;

public interface FinanceService {

    PageResult<CommissionRecordVO> commissionPage(CommissionPageQueryDTO query);

    void settleCommission(String orderNo);

    SettlementBillVO createSettlement(SettlementCreateDTO dto);

    PageResult<SettlementBillVO> settlementPage(SettlementPageQueryDTO query);

    void settleBill(String billNo);

    InvoiceVO createInvoice(InvoiceCreateDTO dto);

    PageResult<InvoiceVO> invoicePage(PageQueryDTO query);

    InvoiceVO getInvoiceByNo(String invoiceNo);

    void issueInvoice(String invoiceNo);

    FinanceSummaryVO getSummary();
}
