package com.platform.water.controller;

import com.platform.common.result.R;
import com.platform.water.dto.Q74PreviewDTO;
import com.platform.water.dto.WaterDispenseCreateDTO;
import com.platform.water.service.WaterDispenseService;
import com.platform.water.vo.Q74ProtocolVO;
import com.platform.water.vo.WaterDispenseOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/water")
@RequiredArgsConstructor
public class WaterDispenseController {

    private final WaterDispenseService waterDispenseService;

    @PostMapping("/scan-orders")
    public R<WaterDispenseOrderVO> createOrder(@Valid @RequestBody WaterDispenseCreateDTO dto) {
        return R.ok(waterDispenseService.createOrder(dto));
    }

    @PostMapping("/scan-orders/{orderNo}/mock-pay")
    public R<WaterDispenseOrderVO> mockPay(@PathVariable String orderNo) {
        return R.ok(waterDispenseService.mockPayAndDispatch(orderNo));
    }

    @GetMapping("/scan-orders/{orderNo}")
    public R<WaterDispenseOrderVO> getOrder(@PathVariable String orderNo) {
        return R.ok(waterDispenseService.getByOrderNo(orderNo));
    }

    @PostMapping("/protocol/q74/preview")
    public R<Q74ProtocolVO> previewQ74(@Valid @RequestBody Q74PreviewDTO dto) {
        return R.ok(waterDispenseService.previewQ74(dto));
    }
}
