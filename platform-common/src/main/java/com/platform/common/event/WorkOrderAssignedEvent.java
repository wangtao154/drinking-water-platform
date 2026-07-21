package com.platform.common.event;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单派单事件，用于触发运维人员通知。
 */
@Data
public class WorkOrderAssignedEvent implements Serializable {

    private Long orderId;

    private String orderNo;

    private String orderType;

    private String orderTypeDesc;

    private String deviceId;

    private String deviceSn;

    private String customerName;

    private String customerPhone;

    private String province;

    private String city;

    private String district;

    private String address;

    private String description;

    private Long workerId;

    private String workerName;

    private String workerPhone;

    private Long dealerId;

    private LocalDateTime assignedAt;
}
