package com.platform.payment.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.payment.dto.PaymentCreateDTO;
import com.platform.payment.dto.PaymentPageQueryDTO;
import com.platform.payment.dto.RefundCreateDTO;
import com.platform.payment.dto.RefundPageQueryDTO;
import com.platform.payment.service.PaymentService;
import com.platform.payment.vo.PaymentRecordVO;
import com.platform.payment.vo.RefundRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public R<PaymentRecordVO> createPayment(@Valid @RequestBody PaymentCreateDTO dto) {
        return R.ok(paymentService.createPayment(dto));
    }

    @GetMapping("/{orderNo}")
    public R<PaymentRecordVO> getByOrderNo(@PathVariable String orderNo) {
        return R.ok(paymentService.getByOrderNo(orderNo));
    }

    @GetMapping
    public R<PageResult<PaymentRecordVO>> page(PaymentPageQueryDTO query) {
        return R.ok(paymentService.page(query));
    }

    @PostMapping("/notify/{orderNo}")
    public R<Void> notify(@PathVariable String orderNo, @RequestBody Map<String, String> body) {
        String transactionId = body.get("transactionId");
        String payStatus = body.get("payStatus");
        String rawResponse = body.get("rawResponse");
        paymentService.handleNotify(orderNo, transactionId, payStatus, rawResponse);
        return R.ok();
    }

    @PostMapping("/refunds")
    public R<RefundRecordVO> createRefund(@Valid @RequestBody RefundCreateDTO dto) {
        return R.ok(paymentService.createRefund(dto));
    }

    @GetMapping("/refunds/{orderNo}")
    public R<RefundRecordVO> getRefundByOrderNo(@PathVariable String orderNo) {
        return R.ok(paymentService.getRefundByOrderNo(orderNo));
    }

    @GetMapping("/refunds")
    public R<PageResult<RefundRecordVO>> pageRefunds(RefundPageQueryDTO query) {
        return R.ok(paymentService.pageRefunds(query));
    }

    @PutMapping("/refunds/{refundNo}/approve")
    public R<Void> approveRefund(@PathVariable String refundNo) {
        paymentService.approveRefund(refundNo);
        return R.ok();
    }

    @PutMapping("/refunds/{refundNo}/reject")
    public R<Void> rejectRefund(@PathVariable String refundNo) {
        paymentService.rejectRefund(refundNo);
        return R.ok();
    }
}
