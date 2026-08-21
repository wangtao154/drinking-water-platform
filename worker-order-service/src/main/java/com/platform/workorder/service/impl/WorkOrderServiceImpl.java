package com.platform.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.UserContext;
import com.platform.common.config.RabbitMqConfig;
import com.platform.common.event.WorkOrderAssignedEvent;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.workorder.dto.*;
import com.platform.workorder.entity.WorkOrder;
import com.platform.workorder.entity.WorkOrderLog;
import com.platform.workorder.enums.OperatorType;
import com.platform.workorder.enums.WorkOrderStatus;
import com.platform.workorder.enums.WorkOrderType;
import com.platform.workorder.mapper.WorkOrderLogMapper;
import com.platform.workorder.mapper.WorkOrderMapper;
import com.platform.workorder.service.WorkOrderService;
import com.platform.workorder.vo.WorkOrderLogVO;
import com.platform.workorder.vo.WorkOrderStatsVO;
import com.platform.workorder.vo.WorkOrderVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 工单编号序号（每日重置）
     */
    private static final AtomicInteger ORDER_SEQ = new AtomicInteger(0);
    private static volatile String lastOrderDate = "";

    @PostConstruct
    public void initOrderSeq() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "WO" + today;
        try {
            String maxOrderNo = jdbcTemplate.queryForObject(
                    "SELECT MAX(order_no) FROM work_order WHERE order_no LIKE ?",
                    String.class,
                    prefix + "%");
            int maxSeq = 0;
            if (StringUtils.hasText(maxOrderNo) && maxOrderNo.length() >= 14) {
                maxSeq = Integer.parseInt(maxOrderNo.substring(maxOrderNo.length() - 4));
            }
            lastOrderDate = today;
            ORDER_SEQ.set(maxSeq);
            log.info("[WorkOrderService] 初始化工单编号序号: date={}, seq={}", today, maxSeq);
        } catch (Exception e) {
            lastOrderDate = today;
            ORDER_SEQ.set(0);
            log.warn("[WorkOrderService] 初始化工单编号序号失败，使用默认序号: date={}", today, e);
        }
    }

    @Override
    @Transactional
    public WorkOrderVO create(WorkOrderCreateDTO dto, Long operatorId) {
        WorkOrder order = new WorkOrder();
        order.setOrderNo(generateOrderNo());
        order.setOrderType(dto.getOrderType());
        order.setDeviceId(dto.getDeviceId());
        order.setCustomerId(dto.getCustomerId());
        order.setDealerId(dto.getDealerId());
        order.setDescription(dto.getDescription());
        order.setAppointTime(dto.getAppointTime());
        order.setTriggerType(dto.getTriggerType() != null ? dto.getTriggerType() : "MANUAL");
        order.setOrderStatus(WorkOrderStatus.PENDING.name());

        // 查询设备信息补充字段
        fillDeviceInfo(order, dto.getDeviceId());

        // 查询客户信息
        fillCustomerInfo(order, dto.getCustomerId());

        workOrderMapper.insert(order);
        log.info("[WorkOrderService] 创建工单: orderNo={}, type={}", order.getOrderNo(), order.getOrderType());

        // 记录日志
        saveLog(order.getId(), null, WorkOrderStatus.PENDING.name(), operatorId, "创建工单");

        return toVO(order);
    }

    @Override
    @Transactional
    public WorkOrderVO createFilterReplace(WorkOrderFilterReplaceDTO dto) {
        WorkOrder order = new WorkOrder();
        order.setOrderNo(generateOrderNo());
        order.setOrderType(WorkOrderType.FILTER_REPLACE.name());
        order.setDeviceId(dto.getDeviceId());
        order.setCustomerId(dto.getCustomerId());
        order.setDealerId(dto.getDealerId());
        order.setOldFilterId(dto.getOldFilterId());
        order.setNewFilterId(dto.getNewFilterId());
        order.setTriggerType("AUTO");
        order.setOrderStatus(WorkOrderStatus.PENDING.name());
        order.setDescription("滤芯到期自动更换：旧滤芯=" + dto.getOldFilterId() + "，新滤芯=" + dto.getNewFilterId());

        // 查询设备信息
        fillDeviceInfo(order, dto.getDeviceId());

        // 查询客户信息
        fillCustomerInfo(order, dto.getCustomerId());

        workOrderMapper.insert(order);
        log.info("[WorkOrderService] 系统自动创建滤芯更换工单: orderNo={}, deviceId={}, oldFilter={}, newFilter={}",
                order.getOrderNo(), dto.getDeviceId(), dto.getOldFilterId(), dto.getNewFilterId());

        // 记录日志（系统操作）
        saveLog(order.getId(), null, WorkOrderStatus.PENDING.name(), 0L, "系统自动创建滤芯更换工单");

        return toVO(order);
    }

    @Override
    @Transactional
    public void dispatch(Long orderId, Long workerId) {
        WorkOrder order = getByIdOrThrow(orderId);

        // 状态校验：只有 PENDING 状态才能派单
        if (!WorkOrderStatus.PENDING.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_CONFLICT,
                    "当前工单状态不允许派单，当前状态：" + order.getOrderStatus());
        }

        // 查询运维人员姓名和电话
        Map<String, String> workerInfo = queryWorkerInfo(workerId);
        if (workerInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "运维人员不存在");
        }

        String workerName = workerInfo.get("name");
        String workerPhone = workerInfo.get("phone");

        // 更新工单
        order.setWorkerId(workerId);
        order.setWorkerName(workerName);
        order.setWorkerPhone(workerPhone);
        order.setOrderStatus(WorkOrderStatus.ASSIGNED.name());
        order.setDispatchNotifyStatus("PENDING");
        order.setDispatchNotifyMessage(null);
        order.setDispatchNotifiedAt(null);
        workOrderMapper.updateById(order);
        publishAssignedEventAfterCommit(order);

        // 记录日志
        CurrentUser currentUser = UserContext.get();
        Long operatorId = currentUser != null ? currentUser.getUserId() : 0L;
        saveLog(orderId, WorkOrderStatus.PENDING.name(), WorkOrderStatus.ASSIGNED.name(),
                operatorId, "派单给运维人员：" + workerName);

        log.info("[WorkOrderService] 派单: orderId={}, workerId={}, workerName={}", orderId, workerId, workerName);
    }
    private void publishAssignedEventAfterCommit(WorkOrder order) {
        WorkOrderAssignedEvent event = buildAssignedEvent(order);
        Runnable publisher = () -> {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.WORKORDER_EXCHANGE,
                        RabbitMqConfig.RK_WORK_ORDER_ASSIGNED,
                        event);
                log.info("[WorkOrderService] 已发布派单通知事件: orderId={}, workerId={}",
                        event.getOrderId(), event.getWorkerId());
            } catch (Exception e) {
                markDispatchNotifyFailed(order.getId(), "派单事件发布失败：" + e.getMessage());
                log.warn("[WorkOrderService] 派单通知事件发布失败: orderId={}", order.getId(), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
        } else {
            publisher.run();
        }
    }

    private WorkOrderAssignedEvent buildAssignedEvent(WorkOrder order) {
        WorkOrderAssignedEvent event = new WorkOrderAssignedEvent();
        event.setOrderId(order.getId());
        event.setOrderNo(order.getOrderNo());
        event.setOrderType(order.getOrderType());
        event.setOrderTypeDesc(resolveOrderTypeDesc(order.getOrderType()));
        event.setDeviceId(order.getDeviceId());
        event.setDeviceSn(order.getDeviceSn());
        event.setCustomerName(order.getCustomerName());
        event.setCustomerPhone(order.getCustomerPhone());
        event.setProvince(order.getProvince());
        event.setCity(order.getCity());
        event.setDistrict(order.getDistrict());
        event.setAddress(order.getAddress());
        event.setDescription(order.getDescription());
        event.setWorkerId(order.getWorkerId());
        event.setWorkerName(order.getWorkerName());
        event.setWorkerPhone(order.getWorkerPhone());
        event.setDealerId(order.getDealerId());
        event.setAssignedAt(LocalDateTime.now());
        return event;
    }

    private String resolveOrderTypeDesc(String orderType) {
        if (!StringUtils.hasText(orderType)) {
            return "工单";
        }
        try {
            return WorkOrderType.valueOf(orderType).getDescription();
        } catch (IllegalArgumentException e) {
            return orderType;
        }
    }

    private void markDispatchNotifyFailed(Long orderId, String message) {
        try {
            String safeMessage = message != null && message.length() > 256
                    ? message.substring(0, 256)
                    : message;
            jdbcTemplate.update(
                    "UPDATE work_order SET dispatch_notify_status = 'FAILED', dispatch_notify_message = ?, dispatch_notified_at = ? WHERE id = ?",
                    safeMessage, LocalDateTime.now(), orderId);
        } catch (Exception e) {
            log.warn("[WorkOrderService] 标记派单通知失败状态异常: orderId={}", orderId, e);
        }
    }

    @Override
    @Transactional
    public void accept(Long orderId, Long workerId) {
        WorkOrder order = getByIdOrThrow(orderId);

        // 状态校验：只有 ASSIGNED 状态才能接单
        if (!WorkOrderStatus.ASSIGNED.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_CONFLICT,
                    "当前工单状态不允许接单，当前状态：" + order.getOrderStatus());
        }

        // 校验接单人员是否为被指派人员
        if (!workerId.equals(order.getWorkerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有被指派的运维人员才能接单");
        }

        // 更新工单
        order.setOrderStatus(WorkOrderStatus.ACCEPTED.name());
        order.setAcceptedAt(LocalDateTime.now());
        workOrderMapper.updateById(order);

        // 记录日志
        saveLog(orderId, WorkOrderStatus.ASSIGNED.name(), WorkOrderStatus.ACCEPTED.name(),
                workerId, "运维人员接单");

        log.info("[WorkOrderService] 接单: orderId={}, workerId={}", orderId, workerId);
    }

    @Override
    @Transactional
    public void startWork(Long orderId, WorkOrderStartDTO dto) {
        WorkOrder order = getByIdOrThrow(orderId);

        // 状态校验：只有 ACCEPTED 状态才能开始作业
        if (!WorkOrderStatus.ACCEPTED.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_CONFLICT,
                    "当前工单状态不允许开始作业，当前状态：" + order.getOrderStatus());
        }

        // 保存作业前照片
        if (dto.getBeforePhotos() != null && !dto.getBeforePhotos().isEmpty()) {
            // 将作业前照片合并到 photoUrls 字段中（以 before_ 前缀标识）
            List<String> existingPhotos = parsePhotoUrls(order.getPhotoUrls());
            for (String photo : dto.getBeforePhotos()) {
                existingPhotos.add("before:" + photo);
            }
            order.setPhotoUrls(toJsonString(existingPhotos));
        }

        order.setOrderStatus(WorkOrderStatus.IN_PROGRESS.name());
        workOrderMapper.updateById(order);

        // 记录日志
        CurrentUser currentUser = UserContext.get();
        Long operatorId = currentUser != null ? currentUser.getUserId() : 0L;
        saveLog(orderId, WorkOrderStatus.ACCEPTED.name(), WorkOrderStatus.IN_PROGRESS.name(),
                operatorId, "开始作业");

        log.info("[WorkOrderService] 开始作业: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void complete(Long orderId, WorkOrderCompleteDTO dto, Long operatorId) {
        WorkOrder order = getByIdOrThrow(orderId);

        // 状态校验：ASSIGNED、ACCEPTED 或 IN_PROGRESS 状态可以完成
        if (!WorkOrderStatus.ASSIGNED.name().equals(order.getOrderStatus())
                && !WorkOrderStatus.ACCEPTED.name().equals(order.getOrderStatus())
                && !WorkOrderStatus.IN_PROGRESS.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_CONFLICT,
                    "当前工单状态不允许完成，当前状态：" + order.getOrderStatus());
        }

        String fromStatus = order.getOrderStatus();

        // 保存作业后照片（过滤掉 before 前缀的旧照片，只保留 after）
        List<String> photoList = new ArrayList<>();
        if (dto.getAfterPhotos() != null && !dto.getAfterPhotos().isEmpty()) {
            photoList.addAll(dto.getAfterPhotos());
        }
        order.setPhotoUrls(toJsonString(photoList));

        order.setRemark(dto.getRemark());
        order.setRating(dto.getRating());
        order.setReviewContent(dto.getReviewContent());
        order.setOrderStatus(WorkOrderStatus.COMPLETED.name());
        order.setCompletedAt(LocalDateTime.now());

        // 如果是滤芯更换工单，且有 oldFilterId/newFilterId，更新滤芯信息
        if (WorkOrderType.FILTER_REPLACE.name().equals(order.getOrderType())) {
            String oldFilterId = StringUtils.hasText(dto.getOldFilterId()) ? dto.getOldFilterId() : order.getOldFilterId();
            String newFilterId = StringUtils.hasText(dto.getNewFilterId()) ? dto.getNewFilterId() : order.getNewFilterId();

            if (StringUtils.hasText(oldFilterId) && StringUtils.hasText(newFilterId)) {
                updateFilterReplacement(order, oldFilterId, newFilterId, operatorId);
                order.setOldFilterId(oldFilterId);
                order.setNewFilterId(newFilterId);
            }
        }

        workOrderMapper.updateById(order);

        // 记录日志
        saveLog(orderId, fromStatus, WorkOrderStatus.COMPLETED.name(),
                operatorId, "工单完成");

        log.info("[WorkOrderService] 完成工单: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void cancel(Long orderId, WorkOrderCancelDTO dto, Long operatorId) {
        WorkOrder order = getByIdOrThrow(orderId);

        // 已完成或已取消的工单不能取消
        if (WorkOrderStatus.COMPLETED.name().equals(order.getOrderStatus())
                || WorkOrderStatus.CANCELLED.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_CONFLICT,
                    "当前工单状态不允许取消，当前状态：" + order.getOrderStatus());
        }

        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(WorkOrderStatus.CANCELLED.name());
        order.setCancelledAt(LocalDateTime.now());
        order.setRemark(dto.getReason());
        workOrderMapper.updateById(order);

        // 记录日志
        saveLog(orderId, fromStatus, WorkOrderStatus.CANCELLED.name(),
                operatorId, "取消工单：" + dto.getReason());

        log.info("[WorkOrderService] 取消工单: orderId={}, reason={}", orderId, dto.getReason());
    }

    @Override
    public WorkOrderVO getById(Long id) {
        WorkOrder order = getByIdOrThrow(id);
        WorkOrderVO vo = toVO(order);

        // 查询状态流转日志
        LambdaQueryWrapper<WorkOrderLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(WorkOrderLog::getWorkOrderId, id);
        logWrapper.orderByAsc(WorkOrderLog::getCreatedAt);
        List<WorkOrderLog> logs = workOrderLogMapper.selectList(logWrapper);
        vo.setStatusLogs(logs.stream().map(this::toLogVO).collect(java.util.stream.Collectors.toList()));

        return vo;
    }

    @Override
    public PageResult<WorkOrderVO> page(WorkOrderPageQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getOrderStatus())) {
            wrapper.eq(WorkOrder::getOrderStatus, query.getOrderStatus());
        }
        if (StringUtils.hasText(query.getOrderType())) {
            wrapper.eq(WorkOrder::getOrderType, query.getOrderType());
        }
        if (query.getWorkerId() != null) {
            wrapper.eq(WorkOrder::getWorkerId, query.getWorkerId());
        }
        if (query.getDealerId() != null) {
            wrapper.eq(WorkOrder::getDealerId, query.getDealerId());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(WorkOrder::getCustomerId, query.getCustomerId());
        }
        if (StringUtils.hasText(query.getTriggerType())) {
            wrapper.eq(WorkOrder::getTriggerType, query.getTriggerType());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(WorkOrder::getOrderNo, query.getKeyword());
        }
        wrapper.orderByDesc(WorkOrder::getCreatedAt);

        IPage<WorkOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        workOrderMapper.selectPage(page, wrapper);

        return PageResult.of(page.convert(this::toVO));
    }

    @Override
    public WorkOrderStatsVO getStatistics(WorkOrderPageQueryDTO query) {
        WorkOrderStatsVO stats = new WorkOrderStatsVO();

        // 总数
        stats.setTotal(workOrderMapper.selectCount(buildStatsWrapper(query, null)));

        // 各状态计数
        stats.setPending(workOrderMapper.selectCount(
                buildStatsWrapper(query, w -> w.eq(WorkOrder::getOrderStatus, WorkOrderStatus.PENDING.name()))));

        stats.setDispatched(workOrderMapper.selectCount(
                buildStatsWrapper(query, w -> w.eq(WorkOrder::getOrderStatus, WorkOrderStatus.ASSIGNED.name()))));

        stats.setAccepted(workOrderMapper.selectCount(
                buildStatsWrapper(query, w -> w.eq(WorkOrder::getOrderStatus, WorkOrderStatus.ACCEPTED.name()))));

        stats.setInProgress(workOrderMapper.selectCount(
                buildStatsWrapper(query, w -> w.eq(WorkOrder::getOrderStatus, WorkOrderStatus.IN_PROGRESS.name()))));

        // 已完成包含：已完成 + 已核实
        stats.setCompleted(workOrderMapper.selectCount(
                buildStatsWrapper(query, w -> w.in(WorkOrder::getOrderStatus,
                        WorkOrderStatus.COMPLETED.name(), WorkOrderStatus.VERIFIED.name()))));

        stats.setCancelled(workOrderMapper.selectCount(
                buildStatsWrapper(query, w -> w.eq(WorkOrder::getOrderStatus, WorkOrderStatus.CANCELLED.name()))));

        // 平均完成耗时（小时）——已完成和已核实都参与计算
        LambdaQueryWrapper<WorkOrder> avgWrapper = buildStatsWrapper(query, w -> {
            w.in(WorkOrder::getOrderStatus, WorkOrderStatus.COMPLETED.name(), WorkOrderStatus.VERIFIED.name());
            w.isNotNull(WorkOrder::getAcceptedAt);
        });
        List<WorkOrder> completedOrders = workOrderMapper.selectList(avgWrapper);
        if (!completedOrders.isEmpty()) {
            double totalHours = 0;
            int count = 0;
            for (WorkOrder wo : completedOrders) {
                if (wo.getAcceptedAt() != null && wo.getCompletedAt() != null) {
                    long seconds = Duration.between(wo.getAcceptedAt(), wo.getCompletedAt()).toSeconds();
                    if (seconds < 0) {
                        continue;
                    }
                    totalHours += seconds / 3600.0;
                    count++;
                }
            }
            if (count > 0) {
                stats.setAvgCompleteHours(BigDecimal.valueOf(totalHours / count).setScale(2, RoundingMode.HALF_UP));
            } else {
                stats.setAvgCompleteHours(BigDecimal.ZERO);
            }
        } else {
            stats.setAvgCompleteHours(BigDecimal.ZERO);
        }

        return stats;
    }

    /**
     * 构建统计查询的 Wrapper（公共条件 + 可选的额外条件）
     */
    private LambdaQueryWrapper<WorkOrder> buildStatsWrapper(WorkOrderPageQueryDTO query,
                                                            java.util.function.Consumer<LambdaQueryWrapper<WorkOrder>> extra) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getOrderType())) {
            wrapper.eq(WorkOrder::getOrderType, query.getOrderType());
        }
        if (query.getDealerId() != null) {
            wrapper.eq(WorkOrder::getDealerId, query.getDealerId());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(WorkOrder::getCustomerId, query.getCustomerId());
        }
        if (query.getWorkerId() != null) {
            wrapper.eq(WorkOrder::getWorkerId, query.getWorkerId());
        }
        if (StringUtils.hasText(query.getTriggerType())) {
            wrapper.eq(WorkOrder::getTriggerType, query.getTriggerType());
        }
        if (query.getCreatedStartTime() != null) {
            wrapper.ge(WorkOrder::getCreatedAt, query.getCreatedStartTime());
        }
        if (query.getCreatedEndTime() != null) {
            wrapper.lt(WorkOrder::getCreatedAt, query.getCreatedEndTime());
        }
        if (extra != null) {
            extra.accept(wrapper);
        }
        return wrapper;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成工单编号：WO + yyyyMMdd + 4位序号
     */
    private synchronized String generateOrderNo() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(lastOrderDate)) {
            lastOrderDate = today;
            ORDER_SEQ.set(0);
        }
        int seq = ORDER_SEQ.incrementAndGet() % 10000;
        return "WO" + today + String.format("%04d", seq);
    }

    /**
     * 根据ID查询工单，不存在则抛异常
     */
    private WorkOrder getByIdOrThrow(Long id) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 查询设备信息并填充到工单中
     */
    private void fillDeviceInfo(WorkOrder order, String deviceId) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    "SELECT sn, province, city, district, address, model_id, customer_id FROM device WHERE device_id = ? AND deleted = 0",
                    deviceId);
            if (!results.isEmpty()) {
                Map<String, Object> device = results.get(0);
                order.setDeviceSn((String) device.get("sn"));
                order.setProvince((String) device.get("province"));
                order.setCity((String) device.get("city"));
                order.setDistrict((String) device.get("district"));
                order.setAddress((String) device.get("address"));
                Object modelId = device.get("model_id");
                if (modelId != null) {
                    order.setModelId(((Number) modelId).longValue());
                }
                // 如果未指定 customerId，从设备信息获取
                if (order.getCustomerId() == null && device.get("customer_id") != null) {
                    order.setCustomerId(((Number) device.get("customer_id")).longValue());
                }
            }
        } catch (Exception e) {
            log.warn("[WorkOrderService] 查询设备信息失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 查询客户信息并填充到工单中
     */
    private void fillCustomerInfo(WorkOrder order, Long customerId) {
        if (customerId == null) return;
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    "SELECT name, phone, province, city, district, address FROM customer WHERE id = ? AND deleted = 0",
                    customerId);
            if (!results.isEmpty()) {
                Map<String, Object> customer = results.get(0);
                order.setCustomerName((String) customer.get("name"));
                order.setCustomerPhone((String) customer.get("phone"));
                if (!StringUtils.hasText(order.getProvince())) {
                    order.setProvince((String) customer.get("province"));
                }
                if (!StringUtils.hasText(order.getCity())) {
                    order.setCity((String) customer.get("city"));
                }
                if (!StringUtils.hasText(order.getDistrict())) {
                    order.setDistrict((String) customer.get("district"));
                }
                if (!StringUtils.hasText(order.getAddress())) {
                    order.setAddress((String) customer.get("address"));
                }
            }
        } catch (Exception e) {
            log.warn("[WorkOrderService] 查询客户信息失败: customerId={}", customerId, e);
        }
    }

    /**
     * 查询运维人员姓名和电话
     */
    private Map<String, String> queryWorkerInfo(Long workerId) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    "SELECT name, phone FROM worker WHERE id = ? AND deleted = 0",
                    workerId);
            if (!results.isEmpty()) {
                Map<String, Object> row = results.get(0);
                Map<String, String> info = new HashMap<>();
                info.put("name", (String) row.get("name"));
                info.put("phone", (String) row.get("phone"));
                return info;
            }
        } catch (Exception e) {
            log.warn("[WorkOrderService] 查询运维人员信息失败: workerId={}", workerId, e);
        }
        return null;
    }

    /**
     * 滤芯更换：更新滤芯实例状态并记录更换日志
     */
    private void updateFilterReplacement(WorkOrder order, String oldFilterId, String newFilterId, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 旧滤芯状态改为 SCRAPPED
        jdbcTemplate.update(
                "UPDATE filter_instance SET lifecycle_status = 'SCRAPPED', scrapped_at = ? WHERE filter_id = ?",
                now, oldFilterId);

        // 2. 新滤芯状态改为 IN_USE，设置设备ID和安装时间
        jdbcTemplate.update(
                "UPDATE filter_instance SET lifecycle_status = 'IN_USE', current_device_id = ?, installed_at = ?, installed_by = ? WHERE filter_id = ?",
                order.getDeviceId(), now, operatorId, newFilterId);

        // 3. 查询旧滤芯使用信息
        Integer oldUsedDuration = 0;
        Long oldUsedFlow = 0L;
        try {
            List<Map<String, Object>> oldFilterInfo = jdbcTemplate.queryForList(
                    "SELECT used_duration, used_flow FROM filter_instance WHERE filter_id = ?",
                    oldFilterId);
            if (!oldFilterInfo.isEmpty()) {
                Map<String, Object> info = oldFilterInfo.get(0);
                if (info.get("used_duration") != null) {
                    oldUsedDuration = ((Number) info.get("used_duration")).intValue();
                }
                if (info.get("used_flow") != null) {
                    oldUsedFlow = ((Number) info.get("used_flow")).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("[WorkOrderService] 查询旧滤芯使用信息失败: filterId={}", oldFilterId, e);
        }

        // 4. 插入滤芯更换记录
        String photoUrlsJson = order.getPhotoUrls();
        jdbcTemplate.update(
                "INSERT INTO filter_replace_log (device_id, old_filter_id, new_filter_id, work_order_id, replaced_by, replaced_at, old_used_duration, old_used_flow, photo_urls, remark, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                order.getDeviceId(), oldFilterId, newFilterId, order.getId(),
                operatorId, now, oldUsedDuration, oldUsedFlow,
                photoUrlsJson != null ? photoUrlsJson : "[]",
                order.getRemark() != null ? order.getRemark() : "",
                now);

        log.info("[WorkOrderService] 滤芯更换完成: orderId={}, oldFilter={}, newFilter={}",
                order.getId(), oldFilterId, newFilterId);
    }

    /**
     * 保存工单流转日志
     */
    private void saveLog(Long workOrderId, String fromStatus, String toStatus, Long operatorId, String remark) {
        WorkOrderLog workOrderLog = new WorkOrderLog();
        workOrderLog.setWorkOrderId(workOrderId);
        workOrderLog.setFromStatus(fromStatus);
        workOrderLog.setToStatus(toStatus);
        workOrderLog.setOperatorId(operatorId != null ? operatorId : 0L);
        workOrderLog.setRemark(remark);
        workOrderLog.setCreatedAt(LocalDateTime.now());

        // 推断操作人类型和姓名
        CurrentUser currentUser = UserContext.get();
        if (currentUser != null) {
            workOrderLog.setOperatorType(currentUser.getUserType() != null ? currentUser.getUserType() : OperatorType.ADMIN.name());
            workOrderLog.setOperatorName(currentUser.getUserName());
        } else {
            // 系统自动操作
            workOrderLog.setOperatorType(OperatorType.ADMIN.name());
            workOrderLog.setOperatorName("系统");
        }

        workOrderLogMapper.insert(workOrderLog);
    }

    /**
     * 解析照片URL JSON字符串为List
     */
    private List<String> parsePhotoUrls(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("[WorkOrderService] 解析照片URL失败: {}", json, e);
            return new ArrayList<>();
        }
    }

    /**
     * 将List转为JSON字符串
     */
    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 实体转VO
     */
    private WorkOrderVO toVO(WorkOrder entity) {
        WorkOrderVO vo = new WorkOrderVO();
        BeanUtils.copyProperties(entity, vo);

        // 工单类型描述
        try {
            vo.setOrderTypeDesc(WorkOrderType.valueOf(entity.getOrderType()).getDescription());
        } catch (IllegalArgumentException e) {
            vo.setOrderTypeDesc(entity.getOrderType());
        }

        // 工单状态描述
        try {
            vo.setOrderStatusDesc(WorkOrderStatus.valueOf(entity.getOrderStatus()).getDescription());
        } catch (IllegalArgumentException e) {
            vo.setOrderStatusDesc(entity.getOrderStatus());
        }

        // 照片URL列表
        vo.setPhotoUrls(parsePhotoUrls(entity.getPhotoUrls()));

        // 客户图片URL列表
        vo.setCustomerImages(parsePhotoUrls(entity.getCustomerImages()));

        // 耗时（小时）
        if (entity.getAcceptedAt() != null && entity.getCompletedAt() != null) {
            long hours = Duration.between(entity.getAcceptedAt(), entity.getCompletedAt()).toHours();
            vo.setDurationHours(hours);
        }

        return vo;
    }

    /**
     * 日志实体转VO
     */
    private WorkOrderLogVO toLogVO(WorkOrderLog entity) {
        WorkOrderLogVO vo = new WorkOrderLogVO();
        BeanUtils.copyProperties(entity, vo);

        // 状态描述
        if (entity.getFromStatus() != null) {
            try {
                vo.setFromStatusDesc(WorkOrderStatus.valueOf(entity.getFromStatus()).getDescription());
            } catch (IllegalArgumentException e) {
                vo.setFromStatusDesc(entity.getFromStatus());
            }
        }
        if (entity.getToStatus() != null) {
            try {
                vo.setToStatusDesc(WorkOrderStatus.valueOf(entity.getToStatus()).getDescription());
            } catch (IllegalArgumentException e) {
                vo.setToStatusDesc(entity.getToStatus());
            }
        }

        return vo;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkOrder order = getByIdOrThrow(id);
        workOrderMapper.deleteById(id);
        log.info("[WorkOrderService] 删除工单: orderId={}, orderNo={}", id, order.getOrderNo());
    }

    private String resolveCustomerOrderType(String orderType) {
        if (!StringUtils.hasText(orderType)) {
            return WorkOrderType.REPAIR.name();
        }
        String normalized = orderType.trim().toUpperCase(Locale.ROOT);
        if (WorkOrderType.REPAIR.name().equals(normalized)
                || WorkOrderType.REMOVE.name().equals(normalized)
                || WorkOrderType.RELOCATE.name().equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ResultCode.PARAM_INVALID, "报修类型只能选择维修、退机或移机");
    }

    // ==================== C 端工单 API ====================

    @Override
    @Transactional
    public WorkOrderVO customerCreate(WorkOrderCustomerCreateDTO dto, Long customerId) {
        WorkOrder order = new WorkOrder();
        order.setOrderNo(generateOrderNo());
        order.setOrderType(resolveCustomerOrderType(dto.getOrderType()));
        order.setDeviceId(dto.getDeviceId());
        order.setCustomerId(customerId);
        order.setDescription(dto.getDescription());
        order.setTriggerType("MANUAL");
        order.setOrderStatus(WorkOrderStatus.PENDING.name());

        // 客户图片
        if (dto.getCustomerImages() != null && !dto.getCustomerImages().isEmpty()) {
            order.setCustomerImages(toJsonString(dto.getCustomerImages()));
        }

        // 查询设备信息补充字段
        if (StringUtils.hasText(dto.getDeviceId())) {
            fillDeviceInfo(order, dto.getDeviceId());
        }

        // 查询客户信息
        fillCustomerInfo(order, customerId);

        // dealerId 从客户或设备信息获取
        if (order.getDealerId() == null) {
            try {
                List<Map<String, Object>> results = jdbcTemplate.queryForList(
                        "SELECT dealer_id FROM customer WHERE id = ? AND deleted = 0", customerId);
                if (!results.isEmpty() && results.get(0).get("dealer_id") != null) {
                    order.setDealerId(((Number) results.get(0).get("dealer_id")).longValue());
                } else {
                    order.setDealerId(1L); // 默认经销商
                }
            } catch (Exception e) {
                order.setDealerId(1L);
            }
        }

        workOrderMapper.insert(order);
        log.info("[WorkOrderService] 客户创建工单: orderNo={}, customerId={}", order.getOrderNo(), customerId);

        saveLog(order.getId(), null, WorkOrderStatus.PENDING.name(), customerId, "客户提交工单");

        return toVO(order);
    }

    @Override
    public List<WorkOrderVO> getMyOrders(Long customerId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId);
        wrapper.orderByDesc(WorkOrder::getCreatedAt);
        List<WorkOrder> orders = workOrderMapper.selectList(wrapper);
        return orders.stream().map(this::toVO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<WorkOrderVO> getWorkerOrders(Long workerId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getWorkerId, workerId);
        wrapper.in(WorkOrder::getOrderStatus,
                WorkOrderStatus.ASSIGNED.name(),
                WorkOrderStatus.ACCEPTED.name(),
                WorkOrderStatus.IN_PROGRESS.name(),
                WorkOrderStatus.COMPLETED.name(),
                WorkOrderStatus.VERIFIED.name());
        wrapper.orderByDesc(WorkOrder::getCreatedAt);
        List<WorkOrder> orders = workOrderMapper.selectList(wrapper);
        return orders.stream().map(this::toVO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public void review(Long orderId, WorkOrderReviewDTO dto, Long customerId) {
        WorkOrder order = getByIdOrThrow(orderId);

        // 状态校验：只有 COMPLETED 状态才能核查
        if (!WorkOrderStatus.COMPLETED.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_CONFLICT,
                    "当前工单状态不允许核查，当前状态：" + order.getOrderStatus());
        }

        // 校验是否为该客户的工单
        if (!customerId.equals(order.getCustomerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能核查自己的工单");
        }

        order.setRating(dto.getRating());
        order.setReviewContent(dto.getReviewContent());
        order.setOrderStatus(WorkOrderStatus.VERIFIED.name());
        workOrderMapper.updateById(order);

        saveLog(orderId, WorkOrderStatus.COMPLETED.name(), WorkOrderStatus.VERIFIED.name(),
                customerId, "客户核查反馈：评分" + dto.getRating() + "星");

        log.info("[WorkOrderService] 客户核查: orderId={}, rating={}", orderId, dto.getRating());
    }
}
