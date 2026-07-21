package com.platform.workorder.service.impl;

import com.platform.workorder.entity.WorkOrder;
import com.platform.workorder.mapper.WorkOrderMapper;
import com.platform.workorder.service.WorkOrderDispatchService;
import com.platform.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 工单自动派单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderDispatchServiceImpl implements WorkOrderDispatchService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderService workOrderService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void autoDispatch(Long orderId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            log.warn("[AutoDispatch] 工单不存在: orderId={}", orderId);
            return;
        }

        // 只有 PENDING 状态才自动派单
        if (!"PENDING".equals(order.getOrderStatus())) {
            log.info("[AutoDispatch] 工单状态非PENDING，跳过自动派单: orderId={}, status={}",
                    orderId, order.getOrderStatus());
            return;
        }

        // 1. 查询设备所在区域
        String province = order.getProvince();
        String city = order.getCity();

        if (province == null || city == null) {
            // 从设备表补充区域信息
            try {
                List<Map<String, Object>> deviceResults = jdbcTemplate.queryForList(
                        "SELECT province, city FROM device WHERE device_id = ? AND deleted = 0",
                        order.getDeviceId());
                if (!deviceResults.isEmpty()) {
                    Map<String, Object> device = deviceResults.get(0);
                    province = (String) device.get("province");
                    city = (String) device.get("city");
                }
            } catch (Exception e) {
                log.warn("[AutoDispatch] 查询设备区域信息失败: deviceId={}", order.getDeviceId(), e);
            }
        }

        if (province == null || city == null) {
            log.warn("[AutoDispatch] 无法获取设备区域信息，跳过自动派单: orderId={}, deviceId={}",
                    orderId, order.getDeviceId());
            return;
        }

        // 2. 查询该区域下状态为 ACTIVE 的运维人员，按当前进行中工单数升序排序
        String sql = "SELECT w.id, w.name, " +
                "(SELECT COUNT(*) FROM work_order wo WHERE wo.worker_id = w.id " +
                "AND wo.order_status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') AND wo.deleted = 0) AS active_count " +
                "FROM worker w " +
                "WHERE w.status = 'ACTIVE' AND w.deleted = 0 " +
                "AND w.province = ? AND w.city = ? " +
                "ORDER BY active_count ASC, w.rating DESC, w.service_count ASC " +
                "LIMIT 1";

        List<Map<String, Object>> workers;
        try {
            workers = jdbcTemplate.queryForList(sql, province, city);
        } catch (Exception e) {
            log.error("[AutoDispatch] 查询可用运维人员失败: province={}, city={}", province, city, e);
            return;
        }

        if (workers.isEmpty()) {
            log.info("[AutoDispatch] 该区域暂无可用运维人员: orderId={}, province={}, city={}",
                    orderId, province, city);
            return;
        }

        // 3. 找到运维人员，执行派单
        Map<String, Object> worker = workers.get(0);
        Long workerId = ((Number) worker.get("id")).longValue();
        String workerName = (String) worker.get("name");
        int activeCount = ((Number) worker.get("active_count")).intValue();

        log.info("[AutoDispatch] 自动派单: orderId={}, workerId={}, workerName={}, activeCount={}",
                orderId, workerId, workerName, activeCount);

        workOrderService.dispatch(orderId, workerId);
    }
}
