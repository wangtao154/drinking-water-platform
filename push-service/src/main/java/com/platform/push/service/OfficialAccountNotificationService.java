package com.platform.push.service;

import com.platform.common.event.WorkOrderAssignedEvent;

public interface OfficialAccountNotificationService {

    void notifyWorkOrderAssigned(WorkOrderAssignedEvent event);
}
