package com.platform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_info")
public class OrderInfo extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String customerPhone;

    private String customerWechat;

    private String productName;

    private Long packageId;

    private Long orderAmount;

    private Long faceValue;

    private Long payAmount;

    private String payMethod;

    private String orderStatus;

    private String commissionStatus;

    private Long dealerId;

    private String dealerName;

    private String orderType;

    private LocalDateTime orderedAt;

    private LocalDateTime paidAt;
}
