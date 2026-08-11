package com.platform.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("water_dispense_order")
public class WaterDispenseOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long customerId;

    private String deviceId;

    private String sn;

    private Long targetMl;

    private Integer waterType;

    private Long payAmount;

    private String payStatus;

    private String dispenseStatus;

    private String commandStatus;

    private String q74Payload;

    private String transactionId;

    private String paymentProvider;

    private String prepayId;

    private String wxOpenId;

    private Boolean mockPayment;

    private String refundStatus;

    private String refundNo;

    private String wechatRefundId;

    private Long refundAmount;

    private String refundReason;

    private LocalDateTime refundRequestedAt;

    private LocalDateTime refundSuccessAt;

    private String refundErrorMsg;

    private LocalDateTime paidAt;

    private LocalDateTime commandSentAt;

    private LocalDateTime completedAt;

    private String remark;
}
