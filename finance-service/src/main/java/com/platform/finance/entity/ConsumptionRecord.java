package com.platform.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("consumption_record")
public class ConsumptionRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String deviceId;

    private Long customerId;

    private Long relatedRechargeId;

    private Long consumeFlow;

    private Long consumeAmount;

    private Long revenueAmount;

    private LocalDateTime consumedAt;

    private LocalDateTime createdAt;
}
