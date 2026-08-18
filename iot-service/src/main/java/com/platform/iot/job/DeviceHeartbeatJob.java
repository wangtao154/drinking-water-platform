package com.platform.iot.job;

import com.platform.common.event.DeviceStatusEvent;
import com.platform.iot.entity.Device;
import com.platform.iot.entity.DeviceOnlineLog;
import com.platform.iot.mapper.DeviceLookupMapper;
import com.platform.iot.mapper.DeviceOnlineLogMapper;
import com.platform.iot.service.CommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设备心跳检测定时任务
 *
 * 定期遍历所有未退货且已绑定控制板 SN 的设备，发送 Q66=1 指令等待 ACK：
 * - 收到 ACK → 设备在线
 * - 超时未收到 → 设备离线
 *
 * 与 LWT 互补：LWT 是设备主动断开时触发，心跳是系统主动检测兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHeartbeatJob {

    private final CommandService commandService;
    private final DeviceLookupMapper deviceLookupMapper;
    private final DeviceOnlineLogMapper deviceOnlineLogMapper;
    private final RabbitTemplate rabbitTemplate;

    /** 并发线程池，避免大量设备时阻塞 */
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    /** 同一控制板的心跳检测只能同时执行一次，避免超时任务重叠。 */
    private final Set<String> heartbeatInProgress = ConcurrentHashMap.newKeySet();

    /**
     * 在线设备心跳：每 30 秒检测一次
     * 确认设备仍然在线，掉线则标记离线
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void heartbeatOnlineDevices() {
        List<Device> devices = deviceLookupMapper.selectHeartbeatCandidates(1);

        if (devices.isEmpty()) {
            return;
        }

        log.info("[Heartbeat-Online] 开始在线设备心跳, 数量: {}", devices.size());
        for (Device device : devices) {
            CompletableFuture.runAsync(() -> checkSingleDevice(device), executor);
        }
    }

    /**
     * 离线设备心跳：每 30 秒检测一次。
     * 与在线设备保持一致，避免 MQTT 断线时短周期堆积发布请求。
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void heartbeatOfflineDevices() {
        List<Device> devices = deviceLookupMapper.selectHeartbeatCandidates(0);

        if (devices.isEmpty()) {
            return;
        }

        log.info("[Heartbeat-Offline] 开始离线设备心跳, 数量: {}", devices.size());
        for (Device device : devices) {
            CompletableFuture.runAsync(() -> checkSingleDevice(device), executor);
        }
    }

    private void checkSingleDevice(Device device) {
        String sn = device.getSn();
        if (!heartbeatInProgress.add(sn)) {
            log.debug("[Heartbeat] 检测仍在执行，跳过重复任务 sn={}", sn);
            return;
        }
        try {
            boolean online = commandService.heartbeat(sn);
            updateOnlineStatus(device, online);
        } catch (Exception e) {
            log.warn("[Heartbeat] 设备 {} 心跳检测异常: {}", sn, e.getMessage());
            updateOnlineStatus(device, false);
        } finally {
            heartbeatInProgress.remove(sn);
        }
    }

    /**
     * 更新设备在线状态（仅在状态变化时更新 + 发事件）
     */
    private void updateOnlineStatus(Device device, boolean online) {
        int newStatus = online ? 1 : 0;
        Integer oldStatus = device.getOnlineStatus();

        // 状态没变化，不更新
        if (oldStatus != null && oldStatus == newStatus) {
            return;
        }

        // 更新设备在线状态
        device.setOnlineStatus(newStatus);
        deviceLookupMapper.updateById(device);

        // 记录在线日志
        LocalDateTime occurredAt = LocalDateTime.now();
        DeviceOnlineLog onlineLog = new DeviceOnlineLog();
        onlineLog.setSn(device.getSn());
        onlineLog.setDeviceId(device.getDeviceId());
        onlineLog.setEventType(online ? "ONLINE" : "OFFLINE");
        onlineLog.setOccurredAt(occurredAt);
        deviceOnlineLogMapper.insert(onlineLog);

        log.info("[Heartbeat] 设备状态变更: sn={}, {} -> {}, eventType={}",
                device.getSn(),
                oldStatus == null ? "UNKNOWN" : (oldStatus == 1 ? "ONLINE" : "OFFLINE"),
                online ? "ONLINE" : "OFFLINE",
                online ? "ONLINE" : "OFFLINE");

        // 发 RabbitMQ 事件（push-service 发模板消息给管理员）
        try {
            DeviceStatusEvent event = DeviceStatusEvent.builder()
                    .sn(device.getSn())
                    .deviceId(device.getDeviceId())
                    .status(online ? "ONLINE" : "OFFLINE")
                    .occurredAt(occurredAt)
                    .build();
            rabbitTemplate.convertAndSend("device.exchange", "device.status.change", event);
        } catch (Exception e) {
            log.error("[Heartbeat] 发送设备状态变更事件失败: sn={}", device.getSn(), e);
        }
    }
}
