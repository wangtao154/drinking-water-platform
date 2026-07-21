package com.platform.push.listener;

import com.platform.common.config.RabbitMqConfig;
import com.platform.common.event.WorkOrderAssignedEvent;
import com.platform.push.service.OfficialAccountNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderAssignedListener {

    private final OfficialAccountNotificationService notificationService;

    @RabbitListener(queues = "work_order.assigned.queue")
    public void onAssigned(WorkOrderAssignedEvent event) {
        log.info("[WorkOrderAssignedListener] 收到派单通知事件: orderId={}, workerId={}",
                event != null ? event.getOrderId() : null,
                event != null ? event.getWorkerId() : null);
        notificationService.notifyWorkOrderAssigned(event);
    }
}
