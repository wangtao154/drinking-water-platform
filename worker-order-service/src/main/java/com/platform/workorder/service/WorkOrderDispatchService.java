package com.platform.workorder.service;

/**
 * 工单自动派单服务接口
 */
public interface WorkOrderDispatchService {

    /**
     * 自动派单
     * 1. 从工单中获取设备ID → 查设备表获取区域信息
     * 2. 查询该区域下空闲的运维人员（按当前进行中工单数排序）
     * 3. 找到则自动派单
     */
    void autoDispatch(Long orderId);
}
