package com.platform.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.dto.PageQueryDTO;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.finance.dto.CommissionPageQueryDTO;
import com.platform.finance.dto.InvoiceCreateDTO;
import com.platform.finance.dto.SettlementCreateDTO;
import com.platform.finance.dto.SettlementPageQueryDTO;
import com.platform.finance.entity.*;
import com.platform.finance.mapper.*;
import com.platform.finance.service.FinanceService;
import com.platform.finance.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final CommissionRecordMapper commissionRecordMapper;
    private final SettlementBillMapper settlementBillMapper;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final InvoiceMapper invoiceMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<CommissionRecordVO> commissionPage(CommissionPageQueryDTO query) {
        query.normalize();
        Page<CommissionRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<CommissionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getOrderNo()), CommissionRecord::getOrderNo, query.getOrderNo())
                .eq(query.getDealerId() != null, CommissionRecord::getDealerId, query.getDealerId())
                .eq(StringUtils.hasText(query.getStatus()), CommissionRecord::getStatus, query.getStatus())
                .orderByDesc(CommissionRecord::getCreatedAt);
        Page<CommissionRecord> result = commissionRecordMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(),
                result.getRecords().stream().map(CommissionRecordVO::fromEntity).toList());
    }

    @Override
    @Transactional
    public void settleCommission(String orderNo) {
        CommissionRecord entity = commissionRecordMapper.selectOne(
                new LambdaQueryWrapper<CommissionRecord>().eq(CommissionRecord::getOrderNo, orderNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分润记录不存在: " + orderNo);
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "分润记录状态不允许结算，当前状态: " + entity.getStatus());
        }
        commissionRecordMapper.update(null,
                new LambdaUpdateWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getOrderNo, orderNo)
                        .set(CommissionRecord::getStatus, "SETTLED")
                        .set(CommissionRecord::getSettledAt, LocalDateTime.now()));
        log.info("Commission settled: orderNo={}", orderNo);
    }

    @Override
    @Transactional
    public SettlementBillVO createSettlement(SettlementCreateDTO dto) {
        LocalDate startDate = LocalDate.parse(dto.getBillStartDate());
        LocalDate endDate = LocalDate.parse(dto.getBillEndDate());

        // Query commission records for the period
        Long totalCommission = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record WHERE dealer_id = ? AND status = 'PENDING'",
                Long.class, dto.getDealerId());

        Long totalAmount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(order_amount), 0) FROM commission_record WHERE dealer_id = ? AND status = 'PENDING'",
                Long.class, dto.getDealerId());

        SettlementBill entity = new SettlementBill();
        entity.setDealerId(dto.getDealerId());
        entity.setBillStartDate(startDate);
        entity.setBillEndDate(endDate);
        entity.setTotalAmount(totalAmount != null ? totalAmount : 0L);
        entity.setCommissionAmount(totalCommission != null ? totalCommission : 0L);
        entity.setWithdrawAmount(0L);
        entity.setStatus("PENDING");

        String billNo = "SB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        entity.setBillNo(billNo);

        settlementBillMapper.insert(entity);
        log.info("Settlement bill created: billNo={}, dealerId={}, commission={}", billNo, dto.getDealerId(), totalCommission);

        return SettlementBillVO.fromEntity(entity);
    }

    @Override
    public PageResult<SettlementBillVO> settlementPage(SettlementPageQueryDTO query) {
        query.normalize();
        Page<SettlementBill> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SettlementBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getDealerId() != null, SettlementBill::getDealerId, query.getDealerId())
                .eq(StringUtils.hasText(query.getStatus()), SettlementBill::getStatus, query.getStatus())
                .orderByDesc(SettlementBill::getCreatedAt);
        Page<SettlementBill> result = settlementBillMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(),
                result.getRecords().stream().map(SettlementBillVO::fromEntity).toList());
    }

    @Override
    @Transactional
    public void settleBill(String billNo) {
        SettlementBill entity = settlementBillMapper.selectOne(
                new LambdaQueryWrapper<SettlementBill>().eq(SettlementBill::getBillNo, billNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "结算账单不存在: " + billNo);
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "账单状态不允许结算，当前状态: " + entity.getStatus());
        }
        settlementBillMapper.update(null,
                new LambdaUpdateWrapper<SettlementBill>()
                        .eq(SettlementBill::getBillNo, billNo)
                        .set(SettlementBill::getStatus, "SETTLED"));

        // Settle all commission records for this dealer
        commissionRecordMapper.update(null,
                new LambdaUpdateWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getDealerId, entity.getDealerId())
                        .eq(CommissionRecord::getStatus, "PENDING")
                        .set(CommissionRecord::getStatus, "SETTLED")
                        .set(CommissionRecord::getSettledAt, LocalDateTime.now()));

        log.info("Settlement bill settled: billNo={}, dealerId={}", billNo, entity.getDealerId());
    }

    @Override
    @Transactional
    public InvoiceVO createInvoice(InvoiceCreateDTO dto) {
        Invoice entity = new Invoice();
        entity.setCustomerId(dto.getCustomerId());
        entity.setAmount(dto.getAmount());
        entity.setTitle(dto.getTitle());
        entity.setTaxNo(dto.getTaxNo());
        entity.setMailingAddress(dto.getMailingAddress());
        entity.setStatus("PENDING");

        String invoiceNo = "INV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        entity.setInvoiceNo(invoiceNo);

        invoiceMapper.insert(entity);
        log.info("Invoice created: invoiceNo={}, customerId={}, amount={}", invoiceNo, dto.getCustomerId(), dto.getAmount());
        return InvoiceVO.fromEntity(entity);
    }

    @Override
    public InvoiceVO getInvoiceByNo(String invoiceNo) {
        Invoice entity = invoiceMapper.selectOne(
                new LambdaQueryWrapper<Invoice>().eq(Invoice::getInvoiceNo, invoiceNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发票不存在: " + invoiceNo);
        }
        return InvoiceVO.fromEntity(entity);
    }

    @Override
    public PageResult<InvoiceVO> invoicePage(PageQueryDTO query) {
        Page<Invoice> page = new Page<>(query.getPageNum(), query.getPageSize());
        invoiceMapper.selectPage(page,
                new LambdaQueryWrapper<Invoice>().orderByDesc(Invoice::getCreatedAt));
        return new PageResult<>(page.getTotal(), (int) page.getSize(), page.getCurrent(),
                page.getRecords().stream().map(InvoiceVO::fromEntity).toList());
    }

    @Override
    @Transactional
    public void issueInvoice(String invoiceNo) {
        Invoice entity = invoiceMapper.selectOne(
                new LambdaQueryWrapper<Invoice>().eq(Invoice::getInvoiceNo, invoiceNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发票不存在: " + invoiceNo);
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "发票状态不允许此操作，当前状态: " + entity.getStatus());
        }
        invoiceMapper.update(null,
                new LambdaUpdateWrapper<Invoice>()
                        .eq(Invoice::getInvoiceNo, invoiceNo)
                        .set(Invoice::getStatus, "ISSUED")
                        .set(Invoice::getIssuedAt, LocalDateTime.now()));
        log.info("Invoice issued: invoiceNo={}", invoiceNo);
    }

    @Override
    public FinanceSummaryVO getSummary() {
        FinanceSummaryVO vo = new FinanceSummaryVO();

        Map<String, Object> commissionStats = commissionRecordMapper.selectCommissionSummary();
        vo.setTotalCommission(toLong(commissionStats.get("totalCommission")));
        vo.setSettledCommission(toLong(commissionStats.get("settledCommission")));
        vo.setPendingCommission(toLong(commissionStats.get("pendingCommission")));

        Map<String, Object> consumptionStats = consumptionRecordMapper.selectConsumptionSummary();
        vo.setTotalConsumption(toLong(consumptionStats.get("totalConsumption")));
        vo.setTotalRevenue(toLong(consumptionStats.get("totalRevenue")));

        Long pendingSettlements = settlementBillMapper.selectCount(
                new LambdaQueryWrapper<SettlementBill>().eq(SettlementBill::getStatus, "PENDING"));
        vo.setPendingSettlements(pendingSettlements);

        Long totalInvoices = invoiceMapper.selectCount(null);
        Long pendingInvoices = invoiceMapper.selectCount(
                new LambdaQueryWrapper<Invoice>().eq(Invoice::getStatus, "PENDING"));
        vo.setTotalInvoices(totalInvoices);
        vo.setPendingInvoices(pendingInvoices);

        return vo;
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
