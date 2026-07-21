package com.platform.workorder.controller;

import com.platform.common.auth.UserContext;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.workorder.dto.*;
import com.platform.workorder.service.WorkOrderService;
import com.platform.workorder.vo.WorkOrderStatsVO;
import com.platform.workorder.vo.WorkOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单管理 Controller
 */
@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /**
     * 创建工单
     */
    @PostMapping
    public R<WorkOrderVO> create(@Valid @RequestBody WorkOrderCreateDTO dto) {
        Long operatorId = UserContext.getUserId();
        return R.ok(workOrderService.create(dto, operatorId));
    }

    /**
     * 系统自动创建滤芯更换工单
     */
    @PostMapping("/filter-replace")
    public R<WorkOrderVO> createFilterReplace(@RequestBody WorkOrderFilterReplaceDTO dto) {
        return R.ok(workOrderService.createFilterReplace(dto));
    }

    /**
     * 查询工单详情
     */
    @GetMapping("/{id}")
    public R<WorkOrderVO> getById(@PathVariable Long id) {
        return R.ok(workOrderService.getById(id));
    }

    /**
     * 派单
     */
    @PutMapping("/{id}/dispatch")
    public R<Void> dispatch(@PathVariable Long id, @Valid @RequestBody WorkOrderDispatchDTO dto) {
        workOrderService.dispatch(id, dto.getWorkerId());
        return R.ok();
    }

    /**
     * 接单
     */
    @PutMapping("/{id}/accept")
    public R<Void> accept(@PathVariable Long id) {
        Long workerId = UserContext.getUserId();
        workOrderService.accept(id, workerId);
        return R.ok();
    }

    /**
     * 开始作业
     */
    @PutMapping("/{id}/start")
    public R<Void> startWork(@PathVariable Long id, @RequestBody WorkOrderStartDTO dto) {
        workOrderService.startWork(id, dto);
        return R.ok();
    }

    /**
     * 完成工单
     */
    @PutMapping("/{id}/complete")
    public R<Void> complete(@PathVariable Long id, @RequestBody WorkOrderCompleteDTO dto) {
        Long operatorId = UserContext.getUserId();
        workOrderService.complete(id, dto, operatorId);
        return R.ok();
    }

    /**
     * 取消工单
     */
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @Valid @RequestBody WorkOrderCancelDTO dto) {
        Long operatorId = UserContext.getUserId();
        workOrderService.cancel(id, dto, operatorId);
        return R.ok();
    }

    /**
     * 分页查询工单
     */
    @GetMapping
    public R<PageResult<WorkOrderVO>> page(WorkOrderPageQueryDTO query) {
        return R.ok(workOrderService.page(query));
    }

    /**
     * 工单统计
     */
    @GetMapping("/statistics")
    public R<WorkOrderStatsVO> getStatistics(WorkOrderPageQueryDTO query) {
        return R.ok(workOrderService.getStatistics(query));
    }

    /**
     * 删除工单
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        workOrderService.delete(id);
        return R.ok();
    }

    // ==================== C 端工单 API ====================

    /**
     * 客户创建工单
     */
    @PostMapping("/customer-create")
    public R<WorkOrderVO> customerCreate(@Valid @RequestBody WorkOrderCustomerCreateDTO dto) {
        Long customerId = UserContext.getUserId();
        return R.ok(workOrderService.customerCreate(dto, customerId));
    }

    /**
     * 查询客户的工单列表
     */
    @GetMapping("/my")
    public R<List<WorkOrderVO>> myOrders() {
        Long customerId = UserContext.getUserId();
        return R.ok(workOrderService.getMyOrders(customerId));
    }

    /**
     * 查询运维人员的工单列表
     */
    @GetMapping("/worker")
    public R<List<WorkOrderVO>> workerOrders() {
        Long workerId = UserContext.getUserId();
        return R.ok(workOrderService.getWorkerOrders(workerId));
    }

    /**
     * 客户核查反馈（COMPLETED → VERIFIED）
     */
    @PutMapping("/{id}/review")
    public R<Void> review(@PathVariable Long id, @Valid @RequestBody WorkOrderReviewDTO dto) {
        Long customerId = UserContext.getUserId();
        workOrderService.review(id, dto, customerId);
        return R.ok();
    }
}
