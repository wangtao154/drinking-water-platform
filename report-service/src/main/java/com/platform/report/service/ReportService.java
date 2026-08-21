package com.platform.report.service;

import com.platform.report.dto.TelemetryExportRequest;
import com.platform.report.vo.DashboardVO;
import com.platform.report.vo.DeviceReportVO;
import com.platform.report.vo.FlowReportVO;
import com.platform.report.vo.FinanceReportVO;
import com.platform.report.vo.OrderReportVO;
import com.platform.report.vo.WorkerReportVO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 报表统计 Service
 */
public interface ReportService {

    DashboardVO getDashboard();
    DashboardVO getDashboard(LocalDateTime startTime, LocalDateTime endTime);
    DeviceReportVO getDeviceReport();
    DeviceReportVO getDeviceReport(LocalDateTime startTime, LocalDateTime endTime);
    OrderReportVO getOrderReport();
    OrderReportVO getOrderReport(LocalDateTime startTime, LocalDateTime endTime);
    FinanceReportVO getFinanceReport();
    WorkerReportVO getWorkerReport();
    FlowReportVO getFlowReport();
    void exportTelemetry(TelemetryExportRequest request, HttpServletResponse response) throws IOException;
}
