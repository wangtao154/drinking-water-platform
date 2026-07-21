package com.platform.workorder.service;

import com.platform.common.result.PageResult;
import com.platform.workorder.dto.*;
import com.platform.workorder.vo.WorkOrderStatsVO;
import com.platform.workorder.vo.WorkOrderVO;

import java.util.List;

/**
 * 工单服务接口
 */
public interface WorkOrderService {

    /**
     * 手动创建工单
     */
    WorkOrderVO create(WorkOrderCreateDTO dto, Long operatorId);

    /**
     * 系统自动创建滤芯更换工单
     */
    WorkOrderVO createFilterReplace(WorkOrderFilterReplaceDTO dto);

    /**
     * 派单（PENDING → ASSIGNED）
     */
    void dispatch(Long orderId, Long workerId);

    /**
     * 师傅接单（ASSIGNED → ACCEPTED）
     */
    void accept(Long orderId, Long workerId);

    /**
     * 开始作业（ACCEPTED → IN_PROGRESS）
     */
    void startWork(Long orderId, WorkOrderStartDTO dto);

    /**
     * 完成工单（IN_PROGRESS → COMPLETED）
     */
    void complete(Long orderId, WorkOrderCompleteDTO dto, Long operatorId);

    /**
     * 取消工单（任意状态 → CANCELLED）
     */
    void cancel(Long orderId, WorkOrderCancelDTO dto, Long operatorId);

    /**
     * 根据ID查询工单详情
     */
    WorkOrderVO getById(Long id);

    /**
     * 分页查询工单
     */
    PageResult<WorkOrderVO> page(WorkOrderPageQueryDTO query);

    /**
     * 工单统计
     */
    WorkOrderStatsVO getStatistics(WorkOrderPageQueryDTO query);

    /**
     * 删除工单（逻辑删除）
     */
    void delete(Long id);

    /**
     * 客户创建工单（C端）
     */
    WorkOrderVO customerCreate(WorkOrderCustomerCreateDTO dto, Long customerId);

    /**
     * 查询客户的工单列表（C端）
     */
    List<WorkOrderVO> getMyOrders(Long customerId);

    /**
     * 查询运维人员的工单列表（C端）
     */
    List<WorkOrderVO> getWorkerOrders(Long workerId);

    /**
     * 客户核查反馈（COMPLETED → VERIFIED）
     */
    void review(Long orderId, WorkOrderReviewDTO dto, Long customerId);
}
