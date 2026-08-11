package com.platform.water.vo;

import com.platform.water.entity.WaterDispenseOrder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaterDispenseOrderVO {

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

    public static WaterDispenseOrderVO fromEntity(WaterDispenseOrder entity) {
        if (entity == null) return null;
        WaterDispenseOrderVO vo = new WaterDispenseOrderVO();
        vo.setId(entity.getId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setCustomerId(entity.getCustomerId());
        vo.setDeviceId(entity.getDeviceId());
        vo.setSn(entity.getSn());
        vo.setTargetMl(entity.getTargetMl());
        vo.setWaterType(entity.getWaterType());
        vo.setPayAmount(entity.getPayAmount());
        vo.setPayStatus(entity.getPayStatus());
        vo.setDispenseStatus(entity.getDispenseStatus());
        vo.setCommandStatus(entity.getCommandStatus());
        vo.setQ74Payload(entity.getQ74Payload());
        vo.setTransactionId(entity.getTransactionId());
        vo.setPaymentProvider(entity.getPaymentProvider());
        vo.setMockPayment(entity.getMockPayment());
        vo.setRefundStatus(entity.getRefundStatus());
        vo.setRefundNo(entity.getRefundNo());
        vo.setWechatRefundId(entity.getWechatRefundId());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setRefundReason(entity.getRefundReason());
        vo.setRefundRequestedAt(entity.getRefundRequestedAt());
        vo.setRefundSuccessAt(entity.getRefundSuccessAt());
        vo.setRefundErrorMsg(entity.getRefundErrorMsg());
        vo.setPaidAt(entity.getPaidAt());
        vo.setCommandSentAt(entity.getCommandSentAt());
        vo.setCompletedAt(entity.getCompletedAt());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
