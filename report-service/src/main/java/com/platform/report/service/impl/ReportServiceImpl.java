package com.platform.report.service.impl;

import com.platform.report.service.ReportService;
import com.platform.report.vo.DashboardVO;
import com.platform.report.vo.DeviceReportVO;
import com.platform.report.vo.FlowReportVO;
import com.platform.report.vo.FinanceReportVO;
import com.platform.report.vo.OrderReportVO;
import com.platform.report.vo.WorkerReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表统计 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setTotalDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0"));
        vo.setOnlineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 1 AND deleted = 0"));
        vo.setOfflineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 0 AND deleted = 0"));
        vo.setTotalCustomers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM customer WHERE deleted = 0"));
        vo.setTotalOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE deleted = 0"));
        vo.setTotalRevenue(queryForLong("SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0"));
        vo.setPendingOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'PENDING' AND deleted = 0"));
        vo.setTotalFilters(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM filter_instance WHERE deleted = 0"));
        vo.setTotalWorkers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM worker WHERE deleted = 0"));
        vo.setTotalDealers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM dealer WHERE deleted = 0"));
        vo.setActiveAlerts(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device_alert WHERE handled_status = 'UNHANDLED' AND deleted = 0"));
        log.info("[报表] 仪表盘统计完成");
        return vo;
    }

    @Override
    public DeviceReportVO getDeviceReport() {
        DeviceReportVO vo = new DeviceReportVO();
        vo.setTotalDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0"));
        vo.setOnlineCount(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 1 AND deleted = 0"));
        vo.setOfflineCount(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 0 AND deleted = 0"));
        vo.setFaultCount(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE lifecycle_status = 'FAULT' AND deleted = 0"));
        vo.setRegisteredToday(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE DATE(created_at) = CURDATE() AND deleted = 0"));
        vo.setActivatedToday(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE DATE(activated_at) = CURDATE() AND deleted = 0"));
        log.info("[报表] 设备统计完成");
        return vo;
    }

    @Override
    public OrderReportVO getOrderReport() {
        OrderReportVO vo = new OrderReportVO();
        vo.setTotalOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE deleted = 0"));
        vo.setPendingOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'PENDING' AND deleted = 0"));
        vo.setPaidOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0"));
        vo.setCancelledOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'CANCELLED' AND deleted = 0"));
        vo.setRefundedOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE order_status = 'REFUNDED' AND deleted = 0"));
        vo.setTotalRevenue(queryForLong("SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0"));
        vo.setTodayOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM order_info WHERE DATE(created_at) = CURDATE() AND deleted = 0"));
        vo.setTodayRevenue(queryForLong("SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND DATE(created_at) = CURDATE() AND deleted = 0"));
        log.info("[报表] 订单统计完成");
        return vo;
    }

    @Override
    public FinanceReportVO getFinanceReport() {
        FinanceReportVO vo = new FinanceReportVO();
        vo.setTotalCommission(queryForLong("SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record WHERE deleted = 0"));
        vo.setSettledCommission(queryForLong("SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record WHERE status = 'SETTLED' AND deleted = 0"));
        vo.setPendingCommission(queryForLong("SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record WHERE status = 'PENDING' AND deleted = 0"));
        vo.setTotalConsumption(queryForLong("SELECT COALESCE(SUM(revenue_amount), 0) FROM consumption_record WHERE deleted = 0"));
        vo.setTotalRevenue(queryForLong("SELECT COALESCE(SUM(pay_amount), 0) FROM order_info WHERE order_status = 'PAID' AND deleted = 0"));
        vo.setPendingSettlements(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM settlement_bill WHERE status = 'PENDING' AND deleted = 0"));
        log.info("[报表] 财务统计完成");
        return vo;
    }

    @Override
    public WorkerReportVO getWorkerReport() {
        WorkerReportVO vo = new WorkerReportVO();
        vo.setTotalWorkers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM worker WHERE deleted = 0"));
        vo.setActiveWorkers(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM worker WHERE status = 'ACTIVE' AND deleted = 0"));
        vo.setTotalWorkOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM work_order WHERE deleted = 0"));
        vo.setPendingWorkOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM work_order WHERE order_status IN ('PENDING', 'DISPATCHED', 'ACCEPTED') AND deleted = 0"));
        vo.setCompletedWorkOrders(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM work_order WHERE order_status = 'COMPLETED' AND deleted = 0"));
        vo.setAvgRating(queryForLong("SELECT COALESCE(ROUND(AVG(rating)), 0) FROM worker WHERE deleted = 0"));
        log.info("[报表] 维修工统计完成");
        return vo;
    }

    // ==================== 私有方法 ====================

    /**
     * 查询单个 Long 值，null 返回 0
     */
    private Long queryForLong(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public FlowReportVO getFlowReport() {
        FlowReportVO vo = new FlowReportVO();
        vo.setTotalDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE deleted = 0"));
        vo.setOnlineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 1 AND deleted = 0"));
        vo.setOfflineDevices(queryForLong("SELECT COALESCE(COUNT(*), 0) FROM device WHERE online_status = 0 AND deleted = 0"));
        vo.setTodayReportDevices(queryForLong(
                "SELECT COALESCE(COUNT(DISTINCT device_id), 0) FROM consumption_record WHERE DATE(consumed_at) = CURDATE() AND deleted = 0"));
        vo.setTotalWaterFlow(queryForLong(
                "SELECT COALESCE(SUM(consume_flow), 0) FROM consumption_record WHERE deleted = 0"));
        vo.setTodayWaterFlow(queryForLong(
                "SELECT COALESCE(SUM(consume_flow), 0) FROM consumption_record WHERE DATE(consumed_at) = CURDATE() AND deleted = 0"));
        vo.setTotalPureFlow(vo.getTotalWaterFlow());
        vo.setTodayPureFlow(vo.getTodayWaterFlow());

        // 在线率
        long total = vo.getTotalDevices() != null ? vo.getTotalDevices() : 0L;
        long online = vo.getOnlineDevices() != null ? vo.getOnlineDevices() : 0L;
        vo.setOnlineRate(total > 0 ? Math.round(online * 1000.0 / total) / 10.0 : 0.0);

        // 近7天用水量趋势
        List<Map<String, Object>> dailyFlow = new ArrayList<>();
        List<Map<String, Object>> flowRows = jdbcTemplate.queryForList(
                "SELECT DATE(consumed_at) as flow_date, COALESCE(SUM(consume_flow), 0) as total_flow " +
                "FROM consumption_record WHERE consumed_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND deleted = 0 " +
                "GROUP BY DATE(consumed_at) ORDER BY flow_date");
        java.time.LocalDate today = java.time.LocalDate.now();
        Map<String, Long> flowMap = new HashMap<>();
        for (Map<String, Object> row : flowRows) {
            flowMap.put(String.valueOf(row.get("flow_date")),
                    row.get("total_flow") != null ? ((Number) row.get("total_flow")).longValue() : 0L);
        }
        for (int i = 6; i >= 0; i--) {
            String dateStr = today.minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);
            item.put("value", flowMap.getOrDefault(dateStr, 0L));
            dailyFlow.add(item);
        }
        vo.setDailyFlowTrend(dailyFlow);

        // 设备型号分布
        List<Map<String, Object>> modelDist = new ArrayList<>();
        List<Map<String, Object>> modelRows = jdbcTemplate.queryForList(
                "SELECT dm.model_name, COUNT(d.id) as cnt FROM device d " +
                "JOIN device_model dm ON d.model_id = dm.id AND dm.deleted = 0 " +
                "WHERE d.deleted = 0 GROUP BY dm.model_name ORDER BY cnt DESC LIMIT 10");
        for (Map<String, Object> row : modelRows) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", row.get("model_name"));
            item.put("value", row.get("cnt"));
            modelDist.add(item);
        }
        vo.setModelDistribution(modelDist);

        log.info("[报表] 流量统计完成");
        return vo;
    }
}
