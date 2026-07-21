package com.platform.payment.vo;

import com.platform.payment.entity.PaymentRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRecordVO extends PaymentRecord {

    public static PaymentRecordVO fromEntity(PaymentRecord entity) {
        if (entity == null) return null;
        PaymentRecordVO vo = new PaymentRecordVO();
        vo.setId(entity.getId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setPayMethod(entity.getPayMethod());
        vo.setTransactionId(entity.getTransactionId());
        vo.setPayAmount(entity.getPayAmount());
        vo.setPayStatus(entity.getPayStatus());
        vo.setPaidAt(entity.getPaidAt());
        vo.setRawResponse(entity.getRawResponse());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
