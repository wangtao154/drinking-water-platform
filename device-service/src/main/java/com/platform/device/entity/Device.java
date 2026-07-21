package com.platform.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
public class Device extends BaseEntity {

    private String deviceId;

    private String sn;

    private Long modelId;

    private String iccid;

    private String imei;

    private LocalDate productionDate;

    private String productionBatch;

    private String qrCodeUrl;

    private Integer onlineStatus;

    private String lifecycleStatus;

    private Long dealerId;

    private Long customerId;

    private LocalDateTime activatedAt;

    private LocalDateTime returnedAt;

    private LocalDateTime scrappedAt;

    private String province;

    private String city;

    private String district;

    private String address;

    private String chargeMode;

    private Integer remainDuration;

    private Long remainFlow;

    private String remark;
}
