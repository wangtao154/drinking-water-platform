package com.platform.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("invoice")
public class Invoice implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String invoiceNo;

    private Long customerId;

    private Long amount;

    private String title;

    private String taxNo;

    private String status;

    private LocalDateTime issuedAt;

    private LocalDateTime mailedAt;

    private String mailingAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
