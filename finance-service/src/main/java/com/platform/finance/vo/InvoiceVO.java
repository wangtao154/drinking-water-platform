package com.platform.finance.vo;

import com.platform.finance.entity.Invoice;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceVO extends Invoice {

    public static InvoiceVO fromEntity(Invoice entity) {
        if (entity == null) return null;
        InvoiceVO vo = new InvoiceVO();
        vo.setId(entity.getId());
        vo.setInvoiceNo(entity.getInvoiceNo());
        vo.setCustomerId(entity.getCustomerId());
        vo.setAmount(entity.getAmount());
        vo.setTitle(entity.getTitle());
        vo.setTaxNo(entity.getTaxNo());
        vo.setStatus(entity.getStatus());
        vo.setIssuedAt(entity.getIssuedAt());
        vo.setMailedAt(entity.getMailedAt());
        vo.setMailingAddress(entity.getMailingAddress());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
