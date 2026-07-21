package com.platform.payment.service;

import com.platform.common.result.PageResult;
import com.platform.payment.dto.PaymentCreateDTO;
import com.platform.payment.dto.PaymentPageQueryDTO;
import com.platform.payment.dto.RefundCreateDTO;
import com.platform.payment.dto.RefundPageQueryDTO;
import com.platform.payment.vo.PaymentRecordVO;
import com.platform.payment.vo.RefundRecordVO;

public interface PaymentService {

    PaymentRecordVO createPayment(PaymentCreateDTO dto);

    PaymentRecordVO getByOrderNo(String orderNo);

    PageResult<PaymentRecordVO> page(PaymentPageQueryDTO query);

    void handleNotify(String orderNo, String transactionId, String payStatus, String rawResponse);

    RefundRecordVO createRefund(RefundCreateDTO dto);

    RefundRecordVO getRefundByOrderNo(String orderNo);

    PageResult<RefundRecordVO> pageRefunds(RefundPageQueryDTO query);

    void approveRefund(String refundNo);

    void rejectRefund(String refundNo);
}
