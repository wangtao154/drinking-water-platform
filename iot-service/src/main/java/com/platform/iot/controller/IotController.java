package com.platform.iot.controller;

import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.iot.config.MqttConfig;
import com.platform.iot.config.MqttConfigService;
import com.platform.iot.dto.TelemetryDTO;
import com.platform.iot.entity.CommandLog;
import com.platform.iot.entity.Device;
import com.platform.iot.mapper.DeviceLookupMapper;
import com.platform.iot.service.CommandService;
import com.platform.iot.service.InfluxDbService;
import com.platform.iot.vo.CommandAckResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/api/v1/iot")
@RequiredArgsConstructor
public class IotController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final DeviceLookupMapper deviceLookupMapper;
    private final InfluxDbService influxDbService;
    private final CommandService commandService;
    private final MqttConfig mqttConfig;
    private final MqttConfigService mqttConfigService;

    @Value("${internal.service-token:drinking-water-internal-service-token-change-me}")
    private String internalServiceToken;

    @GetMapping("/devices/{sn}/latest")
    public R<TelemetryDTO> getLatestTelemetry(@PathVariable String sn) {
        log.info("Querying latest telemetry for SN: {}", sn);

        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在或控制板SN未绑定");
        }
        boolean online = device.getOnlineStatus() != null && device.getOnlineStatus() == 1;

        TelemetryDTO dto = new TelemetryDTO();
        dto.setDeviceSn(sn);
        dto.setOnline(online);

        // Query InfluxDB for latest telemetry data
        Map<String, Object> influxData = influxDbService.queryLatestTelemetry(device.getDeviceId(), sn);
        if (influxData != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> points = (Map<String, Object>) influxData.get("points");
            dto.setPoints(points);
            dto.setTimestamp((String) influxData.get("timestamp"));
        }

        return R.ok(dto);
    }

    @GetMapping("/assets/{deviceId}/latest")
    public R<TelemetryDTO> getLatestTelemetryByDeviceId(@PathVariable String deviceId) {
        Device device = requireDeviceByDeviceId(deviceId);
        return buildLatestTelemetry(device);
    }

    /**
     * 公开接口：游客查询设备最新遥测数据（无需登录）
     */
    @GetMapping("/public/devices/{sn}/latest")
    public R<TelemetryDTO> getPublicLatestTelemetry(@PathVariable String sn) {
        log.info("Public query latest telemetry for SN: {}", sn);
        return getLatestTelemetry(sn);
    }

    @GetMapping("/public/assets/{deviceId}/latest")
    public R<TelemetryDTO> getPublicLatestTelemetryByDeviceId(@PathVariable String deviceId) {
        return getLatestTelemetryByDeviceId(deviceId);
    }

    /**
     * Query historical telemetry data for charting.
     *
     * @param sn     device serial number
     * @param range  time range: 1h, 6h, 24h, 7d, 30d (default: 24h)
     * @param fields   comma-separated field names (e.g., "P1,P17,P7"); if empty, returns all fields
     * @param interval aggregation interval, e.g. 1s, 10s, 30s, 1m, 5m, 10m, 30m, 1h, 6h
     * @return time-series data grouped by field
     */
    @GetMapping("/devices/{sn}/history")
    public R<Map<String, Object>> getHistoryTelemetry(
            @PathVariable String sn,
            @RequestParam(value = "range", defaultValue = "24h") String range,
            @RequestParam(value = "fields", required = false) String fields,
            @RequestParam(value = "interval", required = false) String interval) {

        log.info("Querying history telemetry for SN: {}, range: {}, fields: {}, interval: {}",
                sn, range, fields, interval);

        List<String> fieldList = null;
        if (fields != null && !fields.isBlank()) {
            fieldList = Arrays.stream(fields.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }

        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在或控制板SN未绑定");
        }
        Map<String, Object> result = influxDbService.queryHistoryTelemetry(
                device.getDeviceId(), sn, range, fieldList, interval);
        if (result == null) {
            return R.fail(500, "查询历史数据失败");
        }
        return R.ok(result);
    }

    @GetMapping("/assets/{deviceId}/history")
    public R<Map<String, Object>> getHistoryTelemetryByDeviceId(
            @PathVariable String deviceId,
            @RequestParam(value = "range", defaultValue = "24h") String range,
            @RequestParam(value = "fields", required = false) String fields,
            @RequestParam(value = "interval", required = false) String interval) {
        Device device = requireDeviceByDeviceId(deviceId);
        List<String> fieldList = parseFields(fields);
        Map<String, Object> result = influxDbService.queryHistoryTelemetry(
                device.getDeviceId(), device.getSn(), range, fieldList, interval);
        if (result == null) {
            return R.fail(500, "查询历史数据失败");
        }
        return R.ok(result);
    }

    /** Internal aggregate used by the AI assistant. Never exposes raw telemetry. */
    @GetMapping("/internal/assets/{deviceId}/production-summary")
    public R<Map<String, Object>> getInternalProductionSummary(
            @PathVariable String deviceId,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        verifyInternalToken(internalToken);
        if (!endTime.isAfter(startTime)) {
            return R.fail(40001, "结束时间必须晚于开始时间");
        }
        if (Duration.between(startTime, endTime).compareTo(Duration.ofDays(31)) > 0) {
            return R.fail(40001, "制水统计时间范围不能超过31天");
        }
        Device device = requireDeviceByDeviceId(deviceId);
        Map<String, Object> result = influxDbService.queryProductionSummary(
                device.getDeviceId(), startTime, endTime);
        if (result == null) {
            return R.fail(500, "查询制水统计失败");
        }
        result.put("deviceId", device.getDeviceId());
        result.put("sn", device.getSn());
        return R.ok(result);
    }

    @PostMapping("/devices/{sn}/command")
    public CommandLog sendCommand(@PathVariable String sn, @RequestBody Map<String, Object> params) {
        CurrentUser currentUser = UserContext.get();
        Long operatorId = currentUser != null ? currentUser.getUserId() : null;
        return commandService.sendCommand(sn, params, operatorId);
    }

    /**
     * 下发 set 配置指令到设备
     * 主题: api/v2/set/{sn}, QoS 1
     * 用于写入设备寄存器（如 Q34=Mqtt服务器地址, Q54=Mqtt端口）
     */
    @PostMapping("/devices/{sn}/set")
    public R<CommandLog> sendSetCommand(@PathVariable String sn, @RequestBody Map<String, String> body) {
        return doSendSetCommand(sn, body, currentOperatorId());
    }

    @PostMapping("/internal/devices/{sn}/set")
    public R<CommandLog> sendInternalSetCommand(
            @PathVariable String sn,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        verifyInternalToken(internalToken);
        return doSendSetCommand(sn, body, null);
    }

    @PostMapping("/internal/devices/{sn}/set-with-ack")
    public R<CommandAckResultVO> sendInternalSetCommandWithAck(
            @PathVariable String sn,
            @RequestBody Map<String, String> body,
            @RequestParam(value = "maxAttempts", required = false) Integer maxAttempts,
            @RequestParam(value = "ackTimeoutMs", required = false) Long ackTimeoutMs,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        verifyInternalToken(internalToken);
        String pointID = body.get("pointID");
        String value = body.get("value");
        if (pointID == null || pointID.isEmpty()) {
            return R.fail(40001, "pointID 不能为空");
        }
        if (value == null) {
            return R.fail(40001, "value 不能为空");
        }
        return R.ok(commandService.sendSetCommandWithAck(sn, pointID, value, null, maxAttempts, ackTimeoutMs));
    }

    private R<CommandLog> doSendSetCommand(String sn, Map<String, String> body, Long operatorId) {
        String pointID = body.get("pointID");
        String value = body.get("value");
        if (pointID == null || pointID.isEmpty()) {
            return R.fail(40001, "pointID 不能为空");
        }
        if (value == null) {
            return R.fail(40001, "value 不能为空");
        }
        CommandLog log = commandService.sendSetCommand(sn, pointID, value, operatorId);
        return R.ok(log);
    }

    private Long currentOperatorId() {
        CurrentUser currentUser = UserContext.get();
        return currentUser != null ? currentUser.getUserId() : null;
    }

    @GetMapping("/devices/{sn}/online-status")
    public R<Map<String, Object>> getOnlineStatus(@PathVariable String sn) {
        Device device = deviceLookupMapper.selectBySn(sn);
        if (device == null) {
            return R.ok(Map.of("sn", sn, "online", false, "message", "Device not found"));
        }
        boolean online = device.getOnlineStatus() != null && device.getOnlineStatus() == 1;
        return R.ok(Map.of("sn", sn, "online", online, "deviceId", device.getDeviceId()));
    }

    @GetMapping("/assets/{deviceId}/online-status")
    public R<Map<String, Object>> getOnlineStatusByDeviceId(@PathVariable String deviceId) {
        Device device = requireDeviceByDeviceId(deviceId);
        boolean online = device.getOnlineStatus() != null && device.getOnlineStatus() == 1;
        return R.ok(Map.of(
                "deviceId", device.getDeviceId(),
                "sn", device.getSn(),
                "online", online));
    }

    private Device requireDeviceByDeviceId(String deviceId) {
        Device device = deviceLookupMapper.selectByDeviceId(deviceId);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }
        return device;
    }

    private R<TelemetryDTO> buildLatestTelemetry(Device device) {
        boolean online = device.getOnlineStatus() != null && device.getOnlineStatus() == 1;
        TelemetryDTO dto = new TelemetryDTO();
        dto.setDeviceSn(device.getSn());
        dto.setOnline(online);
        Map<String, Object> influxData = influxDbService.queryLatestTelemetry(
                device.getDeviceId(), device.getSn());
        if (influxData != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> points = (Map<String, Object>) influxData.get("points");
            dto.setPoints(points);
            dto.setTimestamp((String) influxData.get("timestamp"));
        }
        return R.ok(dto);
    }

    private List<String> parseFields(String fields) {
        if (fields == null || fields.isBlank()) {
            return null;
        }
        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    /** 查询当前 MQTT 运行状态（被 system-service 调用） */
    @GetMapping("/mqtt/status")
    public R<Map<String, Object>> mqttStatus(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        verifyInternalToken(internalToken);
        Map<String, Object> status = new HashMap<>();
        status.put("running", mqttConfig.isRunning());
        status.put("connected", mqttConfig.isConnected());
        status.put("broker", mqttConfigService.getCurrentConfig().getBroker());
        status.put("clientId", mqttConfigService.getCurrentConfig().getClientId());
        status.put("enabled", mqttConfigService.getCurrentConfig().getEnabled());
        if (mqttConfig.getLastError() != null) {
            status.put("error", mqttConfig.getLastError());
        }
        return R.ok(status);
    }

    /** 重新加载 MQTT 配置并重连（被 system-service 调用） */
    @PostMapping("/mqtt/reload")
    public R<Void> reloadMqtt(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        verifyInternalToken(internalToken);
        log.info("[MQTT] 收到 reload 请求");
        mqttConfig.reload();
        return R.ok(null);
    }

    private void verifyInternalToken(String internalToken) {
        if (internalServiceToken == null || internalServiceToken.isBlank()
                || internalToken == null || !internalServiceToken.equals(internalToken)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "内部接口令牌无效");
        }
    }
}
