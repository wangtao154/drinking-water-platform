package com.platform.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dealer")
public class Dealer extends BaseEntity {

    private String dealerCode;
    private String dealerName;
    private String dealerLevel;
    private Long parentId;
    private String phone;
    private String password;
    private Long rechargeAmount;
    private Long creditBalance;
    private Long totalBalance;
    private BigDecimal commissionRate;
    private String region;
    private String bankAccount;
    /** 联系人姓名（对应前端 contactPerson） */
    private String contactName;
    private String status;
    private LocalDateTime registeredAt;
}
