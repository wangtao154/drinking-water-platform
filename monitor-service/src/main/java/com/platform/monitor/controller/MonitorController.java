package com.platform.monitor.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.monitor.dto.AlertHandleDTO;
import com.platform.monitor.dto.AlertPageQueryDTO;
import com.platform.monitor.service.MonitorService;
import com.platform.monitor.vo.AlertStatsVO;
import com.platform.monitor.vo.AlertVO;
import com.platform.monitor.vo.ThresholdVO;
import com.platform.common.dto.PageQueryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设备监控 Controller
 */
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    /**
     * 分页查询告警
     */
    @GetMapping("/alerts")
    public R<PageResult<AlertVO>> alertPage(AlertPageQueryDTO query) {
        return R.ok(monitorService.alertPage(query));
    }

    /**
     * 根据ID获取告警详情
     */
    @GetMapping("/alerts/{id}")
    public R<AlertVO> getAlertById(@PathVariable Long id) {
        return R.ok(monitorService.getAlertById(id));
    }

    /**
     * 处理告警
     */
    @PutMapping("/alerts/{id}/handle")
    public R<Void> handleAlert(@PathVariable Long id, @Valid @RequestBody AlertHandleDTO dto) {
        monitorService.handleAlert(id, dto);
        return R.ok();
    }

    /**
     * 告警统计
     */
    @GetMapping("/alerts/statistics")
    public R<AlertStatsVO> getAlertStats() {
        return R.ok(monitorService.getAlertStats());
    }

    /**
     * 分页查询阈值
     */
    @GetMapping("/thresholds")
    public R<PageResult<ThresholdVO>> thresholdPage(PageQueryDTO query) {
        return R.ok(monitorService.thresholdPage(query));
    }

    /**
     * 更新阈值
     */
    @PutMapping("/thresholds/{id}")
    public R<ThresholdVO> updateThreshold(@PathVariable Long id, @RequestParam String thresholdValue) {
        return R.ok(monitorService.updateThreshold(id, thresholdValue));
    }
}
