package com.platform.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("settlement_bill")
public class SettlementBill implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String billNo;

    private Long dealerId;

    private LocalDate billStartDate;

    private LocalDate billEndDate;

    private Long totalAmount;

    private Long commissionAmount;

    private Long withdrawAmount;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
