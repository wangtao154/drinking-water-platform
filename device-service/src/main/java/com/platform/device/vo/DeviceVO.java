package com.platform.device.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeviceVO {

    private Long id;

    private String deviceId;

    private String sn;

    private Long modelId;

    private String modelName;

    private String iccid;

    private String imei;

    private LocalDate productionDate;

    private String productionBatch;

    private String qrCodeUrl;

    private Integer onlineStatus;

    private String lifecycleStatus;

    private Long dealerId;

    private Long customerId;

    private String customerName;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
