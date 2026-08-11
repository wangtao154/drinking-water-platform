package com.platform.water.job;

import com.platform.water.service.WaterDispenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaterDispenseRefundJob {

    private final WaterDispenseService waterDispenseService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void autoRefundRecentAbnormalOrders() {
        try {
            waterDispenseService.autoRefundRecentAbnormalOrders();
        } catch (Exception e) {
            log.warn("Scan water auto refund job failed: {}", e.getMessage());
        }
    }
}
