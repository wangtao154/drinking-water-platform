package com.platform.push.listener;

import com.platform.common.event.DeviceStatusEvent;
import com.platform.push.service.OfficialAccountNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 设备上下线事件监听器
 *
 * 监听 iot-service 发布的 device.status.change 事件，
 * 调用 OfficialAccountNotificationService 向已绑定的管理员发送公众号模板消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusListener {

    private final OfficialAccountNotificationService notificationService;

    @RabbitListener(queues = "device.status.change.queue")
    public void onDeviceStatusChange(DeviceStatusEvent event) {
        log.info("[DeviceStatusListener] 收到设备状态变更事件: sn={}, deviceId={}, status={}",
                event != null ? event.getSn() : null,
                event != null ? event.getDeviceId() : null,
                event != null ? event.getStatus() : null);
        notificationService.notifyDeviceStatus(event);
    }
}