package com.platform.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("commission_record")
public class CommissionRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long dealerId;

    private String dealerLevel;

    private Long orderAmount;

    private BigDecimal commissionRate;

    private Long commissionAmount;

    private String status;

    private LocalDateTime settledAt;

    private LocalDateTime createdAt;
}
