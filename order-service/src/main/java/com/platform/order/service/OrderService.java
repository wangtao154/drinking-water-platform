package com.platform.order.service;

import com.platform.common.result.PageResult;
import com.platform.order.dto.OrderCreateDTO;
import com.platform.order.dto.OrderPageQueryDTO;
import com.platform.order.entity.OrderInfo;
import com.platform.order.vo.OrderStatsVO;
import com.platform.order.vo.OrderVO;

public interface OrderService {

    OrderVO createOrder(OrderCreateDTO dto);

    OrderVO getByOrderNo(String orderNo);

    PageResult<OrderVO> page(OrderPageQueryDTO query);

    void cancelOrder(String orderNo, String reason);

    void payOrder(String orderNo, String payMethod, String transactionId);

    OrderStatsVO getStatistics();
}
