package com.platform.report.service;

import com.platform.report.vo.DashboardVO;
import com.platform.report.vo.DeviceReportVO;
import com.platform.report.vo.FlowReportVO;
import com.platform.report.vo.FinanceReportVO;
import com.platform.report.vo.OrderReportVO;
import com.platform.report.vo.WorkerReportVO;

/**
 * 报表统计 Service
 */
public interface ReportService {

    DashboardVO getDashboard();
    DeviceReportVO getDeviceReport();
    OrderReportVO getOrderReport();
    FinanceReportVO getFinanceReport();
    WorkerReportVO getWorkerReport();
    FlowReportVO getFlowReport();
}
