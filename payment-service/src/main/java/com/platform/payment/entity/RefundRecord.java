package com.platform.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private String refundNo;

    private Long refundAmount;

    private String refundReason;

    private String status;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
