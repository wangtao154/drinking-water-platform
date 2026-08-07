package com.platform.push.service;

import com.platform.common.event.DeviceStatusEvent;
import com.platform.common.event.WorkOrderAssignedEvent;

public interface OfficialAccountNotificationService {

    void notifyWorkOrderAssigned(WorkOrderAssignedEvent event);

    /**
     * 设备上下线通知：读取 Redis 中已绑定的管理员 openid，逐一发送模板消息
     */
    void notifyDeviceStatus(DeviceStatusEvent event);
}
