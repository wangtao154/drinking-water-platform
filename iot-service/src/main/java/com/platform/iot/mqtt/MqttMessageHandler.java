package com.platform.iot.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.enums.PointMapping;
import com.platform.iot.entity.CommandLog;
import com.platform.iot.entity.Device;
import com.platform.iot.entity.DeviceAlert;
import com.platform.iot.entity.DeviceOnlineLog;
import com.platform.iot.mapper.CommandLogMapper;
import com.platform.iot.mapper.DeviceAlertMapper;
import com.platform.iot.mapper.DeviceLookupMapper;
import com.platform.iot.mapper.DeviceOnlineLogMapper;
import com.platform.iot.service.InfluxDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final ObjectMapper objectMapper;
    private final InfluxDbService influxDbService;
    private final DeviceLookupMapper deviceLookupMapper;
    private final DeviceOnlineLogMapper deviceOnlineLogMapper;
    private final DeviceAlertMapper deviceAlertMapper;
    private final CommandLogMapper commandLogMapper;
    private final RabbitTemplate rabbitTemplate;

    public void handleTelemetry(String sn, String payload) {
        log.info("Handling telemetry from device SN: {}", sn);

        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            log.warn("Unknown device SN: {}, ignoring telemetry", sn);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);

            // Handle JSON array format: [{"fields": {"P1": 120, ...}, "deviceID": "..."}]
            if (root.isArray() && root.size() > 0) {
                root = root.get(0);
            }

            Map<String, Object> points = new HashMap<>();
            Map<String, String> tags = new HashMap<>();
            tags.put("sn", sn);
            tags.put("device_id", device.getDeviceId());

            // Parse fields object containing P1-P39 / Q1-Q33 key-value pairs
            JsonNode fieldsNode = root.get("fields");
            if (fieldsNode != null && fieldsNode.isObject()) {
                fieldsNode.fields().forEachRemaining(entry -> {
                    String pointId = entry.getKey();  // e.g. "P1", "P2", "Q1", etc.
                    if (pointId.startsWith("P") || pointId.startsWith("Q")) {
                        JsonNode valueNode = entry.getValue();
                        if (valueNode.isNumber()) {
                            points.put(pointId, PointMapping.normalizeValue(pointId, valueNode.asDouble()));
                        } else if (valueNode.isTextual()) {
                            points.put(pointId, valueNode.asText());
                        }
                    }
                });
            } else {
                // Fallback: try legacy format with P/Q sub-objects
                JsonNode pNode = root.get("P");
                if (pNode != null) {
                    pNode.fields().forEachRemaining(entry -> {
                        String pointId = "P" + entry.getKey();
                        points.put(pointId, PointMapping.normalizeValue(pointId, entry.getValue().asDouble()));
                    });
                }

                JsonNode qNode = root.get("Q");
                if (qNode != null) {
                    qNode.fields().forEachRemaining(entry -> {
                        String pointId = "Q" + entry.getKey();
                        JsonNode valueNode = entry.getValue();
                        if (valueNode.isNumber()) {
                            points.put(pointId, valueNode.asDouble());
                        } else if (valueNode.isTextual()) {
                            points.put(pointId, valueNode.asText());
                        }
                    });
                }
            }

            if (points.isEmpty()) {
                log.warn("No telemetry points parsed from payload for SN: {}", sn);
                return;
            }

            // Write to InfluxDB
            influxDbService.writeTelemetry(sn, points, tags);

            // Update device online status to online when telemetry arrives
            if (device.getOnlineStatus() == null || device.getOnlineStatus() != 1) {
                device.setOnlineStatus(1);
                deviceLookupMapper.updateById(device);
            }

            log.info("Telemetry processed for SN {}: {} data points", sn, points.size());

            // Check alert thresholds
            checkAlerts(sn, device, points);

        } catch (Exception e) {
            log.error("Failed to parse telemetry payload for SN {}: {}", sn, e.getMessage(), e);
        }
    }

    private void checkAlerts(String sn, Device device, Map<String, Object> points) {
        // P28 - fault alarm
        Object p28 = points.get("P28");
        if (p28 != null && ((Number) p28).doubleValue() > 0) {
            createAlert(sn, device, "DEVICE_FAULT", "ALARM", "设备故障报警，P28=" + p28);
        }

        // P32 - water shortage: 0 means shortage, non-zero means normal.
        Object p32 = points.get("P32");
        if (p32 != null && ((Number) p32).doubleValue() == 0) {
            createAlert(sn, device, "WATER_SHORTAGE", "ALARM", "缺水报警，P32=" + p32);
        }

        // P33 - leak
        Object p33 = points.get("P33");
        if (p33 != null && ((Number) p33).doubleValue() > 0) {
            createAlert(sn, device, "WATER_LEAK", "ALARM", "漏水报警，P33=" + p33);
        }

        // P34 - low voltage
        Object p34 = points.get("P34");
        if (p34 != null && ((Number) p34).doubleValue() > 0) {
            createAlert(sn, device, "LOW_VOLTAGE", "WARNING", "电压低预警，P34=" + p34);
        }
    }

    private void createAlert(String sn, Device device, String alertType, String alertLevel, String alertMessage) {
        DeviceAlert alert = new DeviceAlert();
        alert.setDeviceId(device.getDeviceId());
        alert.setSn(sn);
        alert.setAlertType(alertType);
        alert.setAlertLevel(alertLevel);
        alert.setAlertMessage(alertMessage);
        alert.setPushStatus("UNPUSHED");
        alert.setHandledStatus("UNHANDLED");
        alert.setTriggeredAt(LocalDateTime.now());
        deviceAlertMapper.insert(alert);

        // Publish RabbitMQ events for alert checking and notification
        try {
            rabbitTemplate.convertAndSend("alert.check", alert);
            rabbitTemplate.convertAndSend("alert.notify", alert);
        } catch (Exception e) {
            log.error("Failed to publish alert events to RabbitMQ: {}", e.getMessage());
        }
    }

    public void handleLwt(String sn, String payload) {
        // LWT payload is plain text: "online" or "offline"
        String status = payload.trim().toLowerCase();
        boolean online = "online".equals(status);
        log.info("Handling LWT for SN: {} -> {}", sn, online ? "ONLINE" : "OFFLINE");

        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            log.warn("Unknown device SN: {}, ignoring LWT", sn);
            return;
        }

        // Record online/offline event
        DeviceOnlineLog onlineLog = new DeviceOnlineLog();
        onlineLog.setSn(sn);
        onlineLog.setDeviceId(device.getDeviceId());
        onlineLog.setEventType(online ? "ONLINE" : "OFFLINE");
        onlineLog.setOccurredAt(LocalDateTime.now());
        deviceOnlineLogMapper.insert(onlineLog);

        // Update device online status
        device.setOnlineStatus(online ? 1 : 0);
        deviceLookupMapper.updateById(device);

        // Write status to InfluxDB
        influxDbService.writeOnlineStatus(sn, online);
    }

    public void handleAck(String sn, String payload) {
        log.info("Handling ACK from device SN: {}", sn);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String messageId = root.has("messageId") ? root.get("messageId").asText() : null;
            String status = root.has("status") ? root.get("status").asText() : "EXECUTED";

            if (messageId == null) {
                log.warn("ACK missing messageId, SN: {}", sn);
                return;
            }

            // Update command_log
            CommandLog commandLog = commandLogMapper.selectById(messageId);
            if (commandLog != null) {
                commandLog.setStatus(status);
                commandLog.setAckReceivedAt(LocalDateTime.now());
                commandLogMapper.updateById(commandLog);
            } else {
                log.warn("Command log not found for messageId: {}", messageId);
            }

        } catch (Exception e) {
            log.error("Failed to parse ACK payload for SN {}: {}", sn, e.getMessage(), e);
        }
    }
}
