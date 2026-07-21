package com.platform.payment.vo;

import com.platform.payment.entity.RefundRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RefundRecordVO extends RefundRecord {

    public static RefundRecordVO fromEntity(RefundRecord entity) {
        if (entity == null) return null;
        RefundRecordVO vo = new RefundRecordVO();
        vo.setId(entity.getId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setRefundNo(entity.getRefundNo());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setRefundReason(entity.getRefundReason());
        vo.setStatus(entity.getStatus());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setProcessedAt(entity.getProcessedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
