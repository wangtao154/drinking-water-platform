package com.platform.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.payment.dto.PaymentCreateDTO;
import com.platform.payment.dto.PaymentPageQueryDTO;
import com.platform.payment.dto.RefundCreateDTO;
import com.platform.payment.dto.RefundPageQueryDTO;
import com.platform.payment.entity.PaymentRecord;
import com.platform.payment.entity.RefundRecord;
import com.platform.payment.mapper.PaymentRecordMapper;
import com.platform.payment.mapper.RefundRecordMapper;
import com.platform.payment.service.PaymentService;
import com.platform.payment.vo.PaymentRecordVO;
import com.platform.payment.vo.RefundRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;

    @Override
    @Transactional
    public PaymentRecordVO createPayment(PaymentCreateDTO dto) {
        PaymentRecord entity = new PaymentRecord();
        entity.setOrderNo(dto.getOrderNo());
        entity.setPayMethod(dto.getPayMethod());
        entity.setPayAmount(dto.getPayAmount());
        entity.setPayStatus("PENDING");

        // Generate transaction ID
        String transactionId = "TX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        entity.setTransactionId(transactionId);

        paymentRecordMapper.insert(entity);
        log.info("Payment record created: orderNo={}, transactionId={}, amount={}",
                dto.getOrderNo(), transactionId, dto.getPayAmount());

        // Simulate immediate payment success (in production, this would be a callback)
        handleNotify(dto.getOrderNo(), transactionId, "SUCCESS", "{\"result\":\"success\",\"simulated\":true}");

        return getByOrderNo(dto.getOrderNo());
    }

    @Override
    public PaymentRecordVO getByOrderNo(String orderNo) {
        PaymentRecord entity = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getOrderNo, orderNo)
                        .orderByDesc(PaymentRecord::getCreatedAt).last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付记录不存在: " + orderNo);
        }
        return PaymentRecordVO.fromEntity(entity);
    }

    @Override
    public PageResult<PaymentRecordVO> page(PaymentPageQueryDTO query) {
        query.normalize();
        Page<PaymentRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getOrderNo()), PaymentRecord::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getPayStatus()), PaymentRecord::getPayStatus, query.getPayStatus())
                .eq(StringUtils.hasText(query.getPayMethod()), PaymentRecord::getPayMethod, query.getPayMethod())
                .orderByDesc(PaymentRecord::getCreatedAt);
        Page<PaymentRecord> result = paymentRecordMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(),
                result.getRecords().stream().map(PaymentRecordVO::fromEntity).toList());
    }

    @Override
    @Transactional
    public void handleNotify(String orderNo, String transactionId, String payStatus, String rawResponse) {
        PaymentRecord entity = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getOrderNo, orderNo)
                        .eq(PaymentRecord::getTransactionId, transactionId));
        if (entity == null) {
            log.warn("Payment notify received but record not found: orderNo={}, transactionId={}", orderNo, transactionId);
            return;
        }
        paymentRecordMapper.update(null,
                new LambdaUpdateWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getId, entity.getId())
                        .set(PaymentRecord::getPayStatus, payStatus)
                        .set(PaymentRecord::getPaidAt, LocalDateTime.now())
                        .set(PaymentRecord::getRawResponse, rawResponse));
        log.info("Payment notify processed: orderNo={}, transactionId={}, status={}", orderNo, transactionId, payStatus);
    }

    @Override
    @Transactional
    public RefundRecordVO createRefund(RefundCreateDTO dto) {
        // Check if a refund already exists for this order
        Long existingCount = refundRecordMapper.selectCount(
                new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getOrderNo, dto.getOrderNo())
                        .in(RefundRecord::getStatus, "PENDING", "APPROVED"));
        if (existingCount > 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "该订单已存在处理中的退款申请");
        }

        RefundRecord entity = new RefundRecord();
        entity.setOrderNo(dto.getOrderNo());
        entity.setRefundAmount(dto.getRefundAmount());
        entity.setRefundReason(dto.getRefundReason());
        entity.setStatus("PENDING");

        // Generate refund number
        String refundNo = "RF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        entity.setRefundNo(refundNo);

        refundRecordMapper.insert(entity);
        log.info("Refund record created: orderNo={}, refundNo={}, amount={}",
                dto.getOrderNo(), refundNo, dto.getRefundAmount());
        return RefundRecordVO.fromEntity(entity);
    }

    @Override
    public RefundRecordVO getRefundByOrderNo(String orderNo) {
        RefundRecord entity = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getOrderNo, orderNo)
                        .orderByDesc(RefundRecord::getCreatedAt).last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款记录不存在: " + orderNo);
        }
        return RefundRecordVO.fromEntity(entity);
    }

    @Override
    public PageResult<RefundRecordVO> pageRefunds(RefundPageQueryDTO query) {
        query.normalize();
        Page<RefundRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getOrderNo()), RefundRecord::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getRefundStatus()), RefundRecord::getStatus, query.getRefundStatus())
                .orderByDesc(RefundRecord::getCreatedAt);
        Page<RefundRecord> result = refundRecordMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(),
                result.getRecords().stream().map(RefundRecordVO::fromEntity).toList());
    }

    @Override
    @Transactional
    public void approveRefund(String refundNo) {
        RefundRecord entity = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getRefundNo, refundNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款记录不存在: " + refundNo);
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "退款记录状态不允许审批，当前状态: " + entity.getStatus());
        }
        refundRecordMapper.update(null,
                new LambdaUpdateWrapper<RefundRecord>()
                        .eq(RefundRecord::getRefundNo, refundNo)
                        .set(RefundRecord::getStatus, "APPROVED")
                        .set(RefundRecord::getProcessedAt, LocalDateTime.now()));
        log.info("Refund approved: refundNo={}", refundNo);
    }

    @Override
    @Transactional
    public void rejectRefund(String refundNo) {
        RefundRecord entity = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getRefundNo, refundNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款记录不存在: " + refundNo);
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "退款记录状态不允许操作，当前状态: " + entity.getStatus());
        }
        refundRecordMapper.update(null,
                new LambdaUpdateWrapper<RefundRecord>()
                        .eq(RefundRecord::getRefundNo, refundNo)
                        .set(RefundRecord::getStatus, "REJECTED")
                        .set(RefundRecord::getProcessedAt, LocalDateTime.now()));
        log.info("Refund rejected: refundNo={}", refundNo);
    }
}
