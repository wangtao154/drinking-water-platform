package com.platform.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.order.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    @Select("SELECT " +
            "COUNT(*) AS totalOrders, " +
            "SUM(CASE WHEN order_status = 'PENDING' THEN 1 ELSE 0 END) AS pendingOrders, " +
            "SUM(CASE WHEN order_status = 'PAID' THEN 1 ELSE 0 END) AS paidOrders, " +
            "SUM(CASE WHEN order_status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledOrders, " +
            "SUM(CASE WHEN order_status = 'REFUNDING' THEN 1 ELSE 0 END) AS refundingOrders, " +
            "SUM(CASE WHEN order_status = 'REFUNDED' THEN 1 ELSE 0 END) AS refundedOrders, " +
            "COALESCE(SUM(CASE WHEN order_status IN ('PAID','REFUNDING','REFUNDED') THEN pay_amount ELSE 0 END), 0) AS totalPaidAmount, " +
            "COALESCE(SUM(CASE WHEN order_status = 'REFUNDED' THEN pay_amount ELSE 0 END), 0) AS totalRefundAmount " +
            "FROM order_info WHERE deleted = 0")
    Map<String, Object> selectOrderStats();
}
