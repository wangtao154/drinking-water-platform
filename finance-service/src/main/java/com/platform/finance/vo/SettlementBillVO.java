package com.platform.finance.vo;

import com.platform.finance.entity.SettlementBill;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SettlementBillVO extends SettlementBill {

    public static SettlementBillVO fromEntity(SettlementBill entity) {
        if (entity == null) return null;
        SettlementBillVO vo = new SettlementBillVO();
        vo.setId(entity.getId());
        vo.setBillNo(entity.getBillNo());
        vo.setDealerId(entity.getDealerId());
        vo.setBillStartDate(entity.getBillStartDate());
        vo.setBillEndDate(entity.getBillEndDate());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setCommissionAmount(entity.getCommissionAmount());
        vo.setWithdrawAmount(entity.getWithdrawAmount());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
