package com.platform.monitor.controller;

import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.monitor.dto.AlertHandleDTO;
import com.platform.monitor.dto.AlertPageQueryDTO;
import com.platform.monitor.service.MonitorService;
import com.platform.monitor.vo.AlertStatsVO;
import com.platform.monitor.vo.AlertVO;
import com.platform.monitor.vo.ThresholdVO;
import com.platform.common.dto.PageQueryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 设备监控 Controller
 */
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @Value("${internal.service-token:}")
    private String internalServiceToken;

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

    /** Read-only aggregate used by the AI assistant service. */
    @GetMapping("/internal/assistant/alerts/statistics")
    public R<AlertStatsVO> internalAssistantAlertStats(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime) {
        verifyInternalToken(internalToken);
        validateTimeRange(startTime, endTime);
        return R.ok(monitorService.getAlertStats(startTime, endTime));
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

    private void verifyInternalToken(String internalToken) {
        if (internalServiceToken == null || internalServiceToken.isBlank()
                || internalToken == null || !internalServiceToken.equals(internalToken)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "内部接口令牌无效");
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if ((startTime == null) != (endTime == null) || (startTime != null && !endTime.isAfter(startTime))) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "时间范围无效");
        }
    }
}
