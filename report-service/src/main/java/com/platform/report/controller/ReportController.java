package com.platform.report.controller;

import com.platform.common.exception.BusinessException;
import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.report.dto.TelemetryExportRequest;
import com.platform.report.service.ReportService;
import com.platform.report.vo.DashboardVO;
import com.platform.report.vo.DeviceReportVO;
import com.platform.report.vo.FlowReportVO;
import com.platform.report.vo.FinanceReportVO;
import com.platform.report.vo.OrderReportVO;
import com.platform.report.vo.WorkerReportVO;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 报表统计 Controller
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Value("${internal.service-token:}")
    private String internalServiceToken;

    @GetMapping("/dashboard")
    public R<DashboardVO> getDashboard() {
        return R.ok(reportService.getDashboard());
    }

    @GetMapping("/devices")
    public R<DeviceReportVO> getDeviceReport() {
        return R.ok(reportService.getDeviceReport());
    }

    @GetMapping("/orders")
    public R<OrderReportVO> getOrderReport() {
        return R.ok(reportService.getOrderReport());
    }

    /** Fixed read-only report endpoints for the AI assistant. */
    @GetMapping("/internal/assistant/dashboard")
    public R<DashboardVO> internalAssistantDashboard(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime) {
        verifyInternalToken(internalToken);
        validateTimeRange(startTime, endTime);
        return R.ok(reportService.getDashboard(startTime, endTime));
    }

    @GetMapping("/internal/assistant/devices")
    public R<DeviceReportVO> internalAssistantDevices(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime) {
        verifyInternalToken(internalToken);
        validateTimeRange(startTime, endTime);
        return R.ok(reportService.getDeviceReport(startTime, endTime));
    }

    @GetMapping("/internal/assistant/orders")
    public R<OrderReportVO> internalAssistantOrders(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime) {
        verifyInternalToken(internalToken);
        validateTimeRange(startTime, endTime);
        return R.ok(reportService.getOrderReport(startTime, endTime));
    }

    @GetMapping("/finance")
    public R<FinanceReportVO> getFinanceReport() {
        return R.ok(reportService.getFinanceReport());
    }

    @GetMapping("/workers")
    public R<WorkerReportVO> getWorkerReport() {
        return R.ok(reportService.getWorkerReport());
    }

    @GetMapping("/flow")
    public R<FlowReportVO> getFlowReport() {
        return R.ok(reportService.getFlowReport());
    }

    @GetMapping("/telemetry/export")
    public void exportTelemetry(@Valid TelemetryExportRequest request, HttpServletResponse response) throws IOException {
        reportService.exportTelemetry(request, response);
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
