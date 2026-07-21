package com.platform.order.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.order.dto.OrderCreateDTO;
import com.platform.order.dto.OrderPageQueryDTO;
import com.platform.order.service.OrderService;
import com.platform.order.vo.OrderStatsVO;
import com.platform.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public R<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.createOrder(dto));
    }

    @GetMapping("/{orderNo}")
    public R<OrderVO> getByOrderNo(@PathVariable String orderNo) {
        return R.ok(orderService.getByOrderNo(orderNo));
    }

    @GetMapping
    public R<PageResult<OrderVO>> page(OrderPageQueryDTO query) {
        return R.ok(orderService.page(query));
    }

    @PutMapping("/{orderNo}/cancel")
    public R<Void> cancel(@PathVariable String orderNo, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        orderService.cancelOrder(orderNo, reason);
        return R.ok();
    }

    @PutMapping("/{orderNo}/pay")
    public R<Void> pay(@PathVariable String orderNo, @RequestBody Map<String, String> body) {
        String payMethod = body.get("payMethod");
        String transactionId = body.get("transactionId");
        orderService.payOrder(orderNo, payMethod, transactionId);
        return R.ok();
    }

    @GetMapping("/statistics")
    public R<OrderStatsVO> statistics() {
        return R.ok(orderService.getStatistics());
    }
}
