package com.platform.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.order.dto.OrderCreateDTO;
import com.platform.order.dto.OrderPageQueryDTO;
import com.platform.order.entity.OrderInfo;
import com.platform.order.mapper.OrderInfoMapper;
import com.platform.order.service.OrderService;
import com.platform.order.vo.OrderStatsVO;
import com.platform.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;

    @Override
    @Transactional
    public OrderVO createOrder(OrderCreateDTO dto) {
        OrderInfo entity = new OrderInfo();
        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerName(dto.getCustomerName());
        entity.setCustomerPhone(dto.getCustomerPhone());
        entity.setCustomerWechat(dto.getCustomerWechat());
        entity.setProductName(dto.getProductName());
        entity.setPackageId(dto.getPackageId());
        entity.setOrderAmount(dto.getOrderAmount());
        entity.setFaceValue(dto.getFaceValue() != null ? dto.getFaceValue() : dto.getOrderAmount());
        entity.setPayAmount(dto.getPayAmount());
        entity.setPayMethod(dto.getPayMethod());
        entity.setOrderType(dto.getOrderType());
        entity.setDealerId(dto.getDealerId());
        entity.setDealerName(dto.getDealerName());
        entity.setOrderStatus("PENDING");
        entity.setCommissionStatus("PENDING");

        // Generate order number: OD + yyyyMMddHHmmss + 4-digit random
        String orderNo = "OD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        entity.setOrderNo(orderNo);
        entity.setOrderedAt(LocalDateTime.now());

        orderInfoMapper.insert(entity);
        log.info("Order created: orderNo={}, customerId={}, amount={}", orderNo, dto.getCustomerId(), dto.getOrderAmount());
        return OrderVO.fromEntity(entity);
    }

    @Override
    public OrderVO getByOrderNo(String orderNo) {
        OrderInfo entity = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在: " + orderNo);
        }
        return OrderVO.fromEntity(entity);
    }

    @Override
    public PageResult<OrderVO> page(OrderPageQueryDTO query) {
        query.normalize();
        Page<OrderInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getOrderNo()), OrderInfo::getOrderNo, query.getOrderNo())
                .eq(query.getCustomerId() != null, OrderInfo::getCustomerId, query.getCustomerId())
                .eq(StringUtils.hasText(query.getOrderStatus()), OrderInfo::getOrderStatus, query.getOrderStatus())
                .eq(StringUtils.hasText(query.getOrderType()), OrderInfo::getOrderType, query.getOrderType())
                .eq(query.getDealerId() != null, OrderInfo::getDealerId, query.getDealerId())
                .ge(query.getStartTime() != null, OrderInfo::getOrderedAt, query.getStartTime())
                .le(query.getEndTime() != null, OrderInfo::getOrderedAt, query.getEndTime())
                .orderByDesc(OrderInfo::getCreatedAt);
        Page<OrderInfo> result = orderInfoMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), (int) result.getSize(), result.getCurrent(),
                result.getRecords().stream().map(OrderVO::fromEntity).toList());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo, String reason) {
        OrderInfo entity = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在: " + orderNo);
        }
        if (!"PENDING".equals(entity.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "只有待支付订单可以取消，当前状态: " + entity.getOrderStatus());
        }
        orderInfoMapper.update(null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
                        .set(OrderInfo::getOrderStatus, "CANCELLED"));
        log.info("Order cancelled: orderNo={}, reason={}", orderNo, reason);
    }

    @Override
    @Transactional
    public void payOrder(String orderNo, String payMethod, String transactionId) {
        OrderInfo entity = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在: " + orderNo);
        }
        if (!"PENDING".equals(entity.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "订单状态不允许支付，当前状态: " + entity.getOrderStatus());
        }
        orderInfoMapper.update(null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
                        .set(OrderInfo::getOrderStatus, "PAID")
                        .set(OrderInfo::getPayMethod, payMethod)
                        .set(OrderInfo::getPaidAt, LocalDateTime.now()));
        log.info("Order paid: orderNo={}, payMethod={}, transactionId={}", orderNo, payMethod, transactionId);
    }

    @Override
    public OrderStatsVO getStatistics() {
        Map<String, Object> statsMap = orderInfoMapper.selectOrderStats();
        OrderStatsVO vo = new OrderStatsVO();
        vo.setTotalOrders(toLong(statsMap.get("totalOrders")));
        vo.setPendingOrders(toLong(statsMap.get("pendingOrders")));
        vo.setPaidOrders(toLong(statsMap.get("paidOrders")));
        vo.setCancelledOrders(toLong(statsMap.get("cancelledOrders")));
        vo.setRefundingOrders(toLong(statsMap.get("refundingOrders")));
        vo.setRefundedOrders(toLong(statsMap.get("refundedOrders")));
        vo.setTotalPaidAmount(toLong(statsMap.get("totalPaidAmount")));
        vo.setTotalRefundAmount(toLong(statsMap.get("totalRefundAmount")));
        return vo;
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
