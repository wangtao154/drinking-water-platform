package com.platform.report.controller;

import com.platform.common.result.R;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 报表统计 Controller
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

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
}
