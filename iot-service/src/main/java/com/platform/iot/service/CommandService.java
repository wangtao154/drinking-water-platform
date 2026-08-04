package com.platform.iot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.iot.entity.CommandLog;
import com.platform.iot.entity.Device;
import com.platform.iot.config.MqttConfig;
import com.platform.iot.mapper.CommandLogMapper;
import com.platform.iot.mapper.DeviceLookupMapper;
import com.platform.iot.vo.CommandAckResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandService {

    private final MqttConfig mqttConfig;
    private final CommandLogMapper commandLogMapper;
    private final DeviceLookupMapper deviceLookupMapper;
    private final ObjectMapper objectMapper;

    /**
     * 下发 set 指令到设备（配置参数写入）
     * <p>
     * 主题: api/v2/set/{sn}, QoS 1
     * Payload: {"messageID":"uuid","pointID":"Q34","value":"cloud.juconyun.com"}
     * 设备收到后将 value 写入 pointID 对应的寄存器。
     * - Q34~Q53: MQTT服务器地址（uint16[20]，最长40字符，写入Q34自动填充后续寄存器）
     * - Q54: MQTT端口（uint16，0时使用默认1883）
     *
     * @param sn        设备 SN
     * @param pointID   点位 ID（如 "Q34" 或 "Q54"）
     * @param value     要写入的值（字符串，如 "cloud.juconyun.com" 或 "1883"）
     * @param operatorId 操作人 ID
     * @return CommandLog 记录
     */
    public CommandLog sendSetCommand(String sn, String pointID, String value, Long operatorId) {
        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            throw new RuntimeException("Device not found for SN: " + sn);
        }

        String messageID = UUID.randomUUID().toString();

        CommandLog commandLog = new CommandLog();
        commandLog.setMessageId(messageID);
        commandLog.setSn(sn);
        commandLog.setDeviceId(device.getDeviceId());
        commandLog.setPointId(pointID);
        commandLog.setValue(value);
        commandLog.setStatus("PENDING");
        commandLog.setOperatorId(operatorId);
        commandLogMapper.insert(commandLog);

        try {
            publishSetPayload(sn, pointID, value, messageID);
            markSentIfPending(messageID);
            CommandLog current = commandLogMapper.selectByMessageId(messageID);
            log.info("[SET] 配置下发成功 SN={}, topic={}, pointID={}, value={}, messageID={}",
                    sn, "api/v2/set/" + sn, pointID, value, messageID);
            return current != null ? current : commandLog;

        } catch (Exception e) {
            markFailed(messageID);
            log.error("[SET] 配置下发失败 SN={}, pointID={}, value={}: {}",
                    sn, pointID, value, e.getMessage(), e);
            throw new RuntimeException("Failed to send set command: " + e.getMessage());
        }
    }

    public CommandAckResultVO sendSetCommandWithAck(String sn, String pointID, String value, Long operatorId,
                                                    Integer maxAttempts, Long ackTimeoutMs) {
        int attemptsLimit = maxAttempts != null && maxAttempts > 0 ? maxAttempts : 3;
        long timeoutMs = ackTimeoutMs != null && ackTimeoutMs > 0 ? ackTimeoutMs : 6000L;
        CommandAckResultVO result = CommandAckResultVO.pending(attemptsLimit, timeoutMs);
        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            throw new RuntimeException("Device not found for SN: " + sn);
        }

        String messageID = UUID.randomUUID().toString();
        CommandLog commandLog = new CommandLog();
        commandLog.setMessageId(messageID);
        commandLog.setSn(sn);
        commandLog.setDeviceId(device.getDeviceId());
        commandLog.setPointId(pointID);
        commandLog.setValue(value);
        commandLog.setStatus("PENDING");
        commandLog.setOperatorId(operatorId);
        commandLogMapper.insert(commandLog);
        result.setLastMessageId(messageID);

        for (int attempt = 1; attempt <= attemptsLimit; attempt++) {
            result.setAttempts(attempt);
            CommandLog existingAck = commandLogMapper.selectByMessageId(messageID);
            if (existingAck != null && existingAck.getAckReceivedAt() != null) {
                return buildAckResult(result, existingAck);
            }

            try {
                publishSetPayload(sn, pointID, value, messageID);
                markSentForRetry(messageID);
                log.info("[SET] 配置下发等待ACK SN={}, topic={}, pointID={}, attempt={}/{}, messageID={}",
                        sn, "api/v2/set/" + sn, pointID, attempt, attemptsLimit, messageID);
            } catch (Exception e) {
                markFailed(messageID);
                result.setStatus("FAILED");
                result.setErrorMessage("Q74下发失败: " + e.getMessage());
                log.error("[SET] 配置下发失败 SN={}, pointID={}, value={}, attempt={}, messageID={}: {}",
                        sn, pointID, value, attempt, messageID, e.getMessage(), e);
                return result;
            }

            CommandLog ackLog = waitForAck(messageID, timeoutMs);
            if (ackLog != null && ackLog.getAckReceivedAt() != null) {
                boolean acknowledged = "ACK".equals(ackLog.getStatus()) || "EXECUTED".equals(ackLog.getStatus());
                result.setAckReceived(true);
                result.setAcknowledged(acknowledged);
                result.setStatus(ackLog.getStatus());
                if (!acknowledged) {
                    result.setErrorMessage("设备返回执行失败");
                }
                return result;
            }
            markTimeoutIfSent(messageID);
        }

        result.setStatus("TIMEOUT");
        result.setErrorMessage("未在指定时间内收到设备ACK");
        return result;
    }

    private CommandAckResultVO buildAckResult(CommandAckResultVO result, CommandLog ackLog) {
        boolean acknowledged = "ACK".equals(ackLog.getStatus()) || "EXECUTED".equals(ackLog.getStatus());
        result.setAckReceived(true);
        result.setAcknowledged(acknowledged);
        result.setStatus(ackLog.getStatus());
        result.setLastMessageId(ackLog.getMessageId());
        if (!acknowledged) {
            result.setErrorMessage("设备返回执行失败");
        }
        return result;
    }

    private void publishSetPayload(String sn, String pointID, String value, String messageID) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageID", messageID);
        payload.put("pointID", pointID);
        payload.put("value", value);

        String payloadJson = objectMapper.writeValueAsString(payload);
        String topic = "api/v2/set/" + sn;
        MqttMessage mqttMessage = new MqttMessage(payloadJson.getBytes());
        mqttMessage.setQos(1);
        mqttConfig.getConnectedClient().publish(topic, mqttMessage);
    }

    private CommandLog waitForAck(String messageId, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        CommandLog commandLog = null;
        while (System.currentTimeMillis() <= deadline) {
            commandLog = commandLogMapper.selectByMessageId(messageId);
            if (commandLog != null && commandLog.getAckReceivedAt() != null) {
                return commandLog;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return commandLog;
            }
        }
        return commandLogMapper.selectByMessageId(messageId);
    }

    private void markSentIfPending(String messageId) {
        commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                .eq(CommandLog::getMessageId, messageId)
                .eq(CommandLog::getStatus, "PENDING")
                .set(CommandLog::getStatus, "SENT"));
    }

    private void markSentForRetry(String messageId) {
        commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                .eq(CommandLog::getMessageId, messageId)
                .ne(CommandLog::getStatus, "ACK")
                .ne(CommandLog::getStatus, "EXECUTED")
                .set(CommandLog::getStatus, "SENT"));
    }

    private void markFailed(String messageId) {
        commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                .eq(CommandLog::getMessageId, messageId)
                .set(CommandLog::getStatus, "FAILED"));
    }

    private void markTimeoutIfSent(String messageId) {
        commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                .eq(CommandLog::getMessageId, messageId)
                .eq(CommandLog::getStatus, "SENT")
                .set(CommandLog::getStatus, "TIMEOUT"));
    }

    public CommandLog sendCommand(String sn, Map<String, Object> params, Long operatorId) {
        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            throw new RuntimeException("Device not found for SN: " + sn);
        }

        String messageId = UUID.randomUUID().toString();

        try {
            // Build MQTT payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("messageId", messageId);
            payload.put("timestamp", System.currentTimeMillis());

            // Add Q-series parameters
            Map<String, Object> qPoints = new HashMap<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("Q")) {
                    qPoints.put(key, entry.getValue());
                }
            }
            if (!qPoints.isEmpty()) {
                payload.put("Q", qPoints);
            }

            String payloadJson = objectMapper.writeValueAsString(payload);
            String topic = "api/v2/download/" + sn;

            // Publish to MQTT
            MqttMessage mqttMessage = new MqttMessage(payloadJson.getBytes());
            mqttMessage.setQos(1);
            mqttConfig.getConnectedClient().publish(topic, mqttMessage);

            // Save to command_log
            // For each Q-series point, create a separate command log entry
            for (Map.Entry<String, Object> entry : qPoints.entrySet()) {
                CommandLog commandLog = new CommandLog();
                commandLog.setMessageId(messageId);
                commandLog.setSn(sn);
                commandLog.setDeviceId(device.getDeviceId());
                commandLog.setPointId(entry.getKey());
                commandLog.setValue(entry.getValue().toString());
                commandLog.setStatus("SENT");
                commandLog.setOperatorId(operatorId);
                commandLogMapper.insert(commandLog);
            }

            log.info("Command sent to device SN: {}, messageId: {}", sn, messageId);
            // Return the first command log as reference
            CommandLog referenceLog = new CommandLog();
            referenceLog.setMessageId(messageId);
            referenceLog.setSn(sn);
            referenceLog.setDeviceId(device.getDeviceId());
            referenceLog.setStatus("SENT");
            return referenceLog;

        } catch (Exception e) {
            log.error("Failed to send command to device SN {}: {}", sn, e.getMessage(), e);
            throw new RuntimeException("Failed to send command: " + e.getMessage());
        }
    }
}
