package com.platform.finance.vo;

import com.platform.finance.entity.CommissionRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommissionRecordVO extends CommissionRecord {

    public static CommissionRecordVO fromEntity(CommissionRecord entity) {
        if (entity == null) return null;
        CommissionRecordVO vo = new CommissionRecordVO();
        vo.setId(entity.getId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setDealerId(entity.getDealerId());
        vo.setDealerLevel(entity.getDealerLevel());
        vo.setOrderAmount(entity.getOrderAmount());
        vo.setCommissionRate(entity.getCommissionRate());
        vo.setCommissionAmount(entity.getCommissionAmount());
        vo.setStatus(entity.getStatus());
        vo.setSettledAt(entity.getSettledAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
