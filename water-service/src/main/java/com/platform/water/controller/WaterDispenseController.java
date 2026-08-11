package com.platform.water.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.water.dto.Q74PreviewDTO;
import com.platform.water.dto.WaterDispenseCreateDTO;
import com.platform.water.dto.WaterDispenseOrderPageQueryDTO;
import com.platform.water.service.WaterDispenseService;
import com.platform.water.vo.Q74ProtocolVO;
import com.platform.water.vo.WaterDispensePricePreviewVO;
import com.platform.water.vo.WaterDispenseOrderVO;
import com.platform.water.vo.WaterDispenseOrderStatsVO;
import com.platform.water.vo.WaterWechatPayVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/water")
@RequiredArgsConstructor
@Slf4j
public class WaterDispenseController {

    private final WaterDispenseService waterDispenseService;

    @PostMapping("/scan-orders")
    public R<WaterDispenseOrderVO> createOrder(@Valid @RequestBody WaterDispenseCreateDTO dto) {
        return R.ok(waterDispenseService.createOrder(dto));
    }

    @GetMapping("/scan-orders/price-preview")
    public R<WaterDispensePricePreviewVO> previewPrice(
            @RequestParam Long targetMl,
            @RequestParam(required = false) Integer waterType) {
        return R.ok(waterDispenseService.previewPrice(targetMl, waterType));
    }

    @PostMapping("/scan-orders/{orderNo}/wechat-pay")
    public R<WaterWechatPayVO> prepareWechatPay(@PathVariable String orderNo) {
        return R.ok(waterDispenseService.prepareWechatPay(orderNo));
    }

    @PostMapping("/scan-orders/{orderNo}/refund/retry")
    public R<WaterDispenseOrderVO> retryRefund(@PathVariable String orderNo) {
        return R.ok(waterDispenseService.retryRefund(orderNo));
    }

    @GetMapping("/scan-orders/{orderNo}")
    public R<WaterDispenseOrderVO> getOrder(@PathVariable String orderNo) {
        return R.ok(waterDispenseService.getByOrderNo(orderNo));
    }

    @GetMapping("/scan-orders")
    public R<PageResult<WaterDispenseOrderVO>> pageOrders(WaterDispenseOrderPageQueryDTO query) {
        return R.ok(waterDispenseService.pageOrders(query));
    }

    @GetMapping("/scan-orders/statistics")
    public R<WaterDispenseOrderStatsVO> orderStats(WaterDispenseOrderPageQueryDTO query) {
        return R.ok(waterDispenseService.getOrderStats(query));
    }

    @PostMapping("/protocol/q74/preview")
    public R<Q74ProtocolVO> previewQ74(@Valid @RequestBody Q74PreviewDTO dto) {
        return R.ok(waterDispenseService.previewQ74(dto));
    }

    @PostMapping("/pay/wechat/notify")
    public ResponseEntity<Map<String, String>> wechatPayNotify(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestBody String body) {
        try {
            waterDispenseService.handleWechatPayNotify(timestamp, nonce, signature, serial, body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "success"));
        } catch (Exception e) {
            log.warn("[WechatPay] notify handling failed, serial={}, message={}", serial, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", "fail"));
        }
    }

    @PostMapping("/pay/wechat/refund/notify")
    public ResponseEntity<Map<String, String>> wechatRefundNotify(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestBody String body) {
        try {
            waterDispenseService.handleWechatRefundNotify(timestamp, nonce, signature, serial, body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "success"));
        } catch (Exception e) {
            log.warn("[WechatPay] refund notify handling failed, serial={}, message={}", serial, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", "fail"));
        }
    }
}
