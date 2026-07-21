package com.platform.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private String payMethod;

    private String transactionId;

    private Long payAmount;

    private String payStatus;

    private LocalDateTime paidAt;

    private String rawResponse;

    private LocalDateTime createdAt;
}
