package com.platform.order.vo;

import com.platform.order.entity.OrderInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderVO extends OrderInfo {

    public static OrderVO fromEntity(OrderInfo entity) {
        if (entity == null) return null;
        OrderVO vo = new OrderVO();
        vo.setId(entity.getId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setCustomerId(entity.getCustomerId());
        vo.setCustomerName(entity.getCustomerName());
        vo.setCustomerPhone(entity.getCustomerPhone());
        vo.setCustomerWechat(entity.getCustomerWechat());
        vo.setProductName(entity.getProductName());
        vo.setPackageId(entity.getPackageId());
        vo.setOrderAmount(entity.getOrderAmount());
        vo.setFaceValue(entity.getFaceValue());
        vo.setPayAmount(entity.getPayAmount());
        vo.setPayMethod(entity.getPayMethod());
        vo.setOrderStatus(entity.getOrderStatus());
        vo.setCommissionStatus(entity.getCommissionStatus());
        vo.setDealerId(entity.getDealerId());
        vo.setDealerName(entity.getDealerName());
        vo.setOrderType(entity.getOrderType());
        vo.setOrderedAt(entity.getOrderedAt());
        vo.setPaidAt(entity.getPaidAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
