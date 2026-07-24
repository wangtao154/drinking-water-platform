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
    private Long payAmount;
    private String payStatus;
    private String dispenseStatus;
    private String commandStatus;
    private String q74Payload;
    private String transactionId;
    private Boolean mockPayment;
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
        vo.setPayAmount(entity.getPayAmount());
        vo.setPayStatus(entity.getPayStatus());
        vo.setDispenseStatus(entity.getDispenseStatus());
        vo.setCommandStatus(entity.getCommandStatus());
        vo.setQ74Payload(entity.getQ74Payload());
        vo.setTransactionId(entity.getTransactionId());
        vo.setMockPayment(entity.getMockPayment());
        vo.setPaidAt(entity.getPaidAt());
        vo.setCommandSentAt(entity.getCommandSentAt());
        vo.setCompletedAt(entity.getCompletedAt());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
