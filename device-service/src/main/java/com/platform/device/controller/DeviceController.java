package com.platform.device.controller;

import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.device.dto.DeviceBindDTO;
import com.platform.device.dto.DeviceQueryDTO;
import com.platform.device.dto.DeviceRegisterDTO;
import com.platform.device.dto.DeviceUpdateDTO;
import com.platform.device.service.DeviceService;
import com.platform.device.vo.DeviceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Value("${internal.service-token:}")
    private String internalServiceToken;

    @PostMapping
    public R<DeviceVO> register(@Valid @RequestBody DeviceRegisterDTO dto) {
        return R.ok(deviceService.register(dto));
    }

    @PutMapping("/{id}")
    public R<DeviceVO> update(@PathVariable String id, @Valid @RequestBody DeviceUpdateDTO dto) {
        return R.ok(deviceService.updateByIdOrDeviceId(id, dto));
    }

    /**
     * 查询设备详情：同时支持数据库主键 Long 和业务 deviceId 字符串
     */
    @GetMapping("/{id}")
    public R<DeviceVO> getDetail(@PathVariable String id) {
        return R.ok(deviceService.getDetailByIdOrDeviceId(id));
    }

    /**
     * 公开接口：游客查询设备详情（无需登录）
     */
    @GetMapping("/public/{deviceId}")
    public R<DeviceVO> getPublicDetail(@PathVariable String deviceId) {
        return R.ok(deviceService.getDeviceByDeviceId(deviceId));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        deviceService.deleteByIdOrDeviceId(id);
        return R.ok();
    }

    @GetMapping
    public R<PageResult<DeviceVO>> page(DeviceQueryDTO query) {
        return R.ok(deviceService.page(query));
    }

    /**
     * Internal assistant lookup. Only a compact device snapshot is exposed;
     * customer, address, and other page fields are intentionally omitted.
     */
    @GetMapping("/internal/assistant/device")
    public R<Map<String, Object>> internalAssistantDevice(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam String identifier) {
        verifyInternalToken(internalToken);
        DeviceQueryDTO query = new DeviceQueryDTO();
        query.setKeyword(identifier == null ? null : identifier.trim());
        query.setPageSize(1);
        List<DeviceVO> records = deviceService.page(query).getRecords();
        if (records == null || records.isEmpty()) {
            return R.ok(Map.of("found", false));
        }
        DeviceVO device = records.get(0);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("found", true);
        snapshot.put("deviceId", device.getDeviceId());
        snapshot.put("sn", device.getSn());
        snapshot.put("modelName", device.getModelName());
        snapshot.put("onlineStatus", device.getOnlineStatus());
        snapshot.put("lifecycleStatus", device.getLifecycleStatus());
        snapshot.put("activatedAt", device.getActivatedAt());
        return R.ok(snapshot);
    }

    @PostMapping("/bind")
    public R<DeviceVO> bind(@Valid @RequestBody DeviceBindDTO dto) {
        Long customerId = UserContext.getUserId();
        return R.ok(deviceService.bind(dto, customerId));
    }

    @PostMapping("/unbind/{deviceId}")
    public R<Void> unbind(@PathVariable String deviceId) {
        deviceService.unbind(deviceId);
        return R.ok();
    }

    @GetMapping("/{deviceId}/qrcode")
    public R<DeviceVO> getQrCode(@PathVariable String deviceId) {
        return R.ok(deviceService.getDeviceByDeviceId(deviceId));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{deviceId}/qrcode/regenerate")
    public R<DeviceVO> regenerateQrCode(@PathVariable String deviceId) {
        return R.ok(deviceService.regenerateQrCode(deviceId));
    }

    @org.springframework.web.bind.annotation.PostMapping("/qrcode/regenerate-all")
    public R<Integer> regenerateAllQrCodes() {
        return R.ok(deviceService.regenerateAllQrCodes());
    }

    /**
     * C 端：查询当前客户的所有设备
     */
    @GetMapping("/my")
    public R<List<DeviceVO>> myDevices() {
        Long customerId = UserContext.getUserId();
        return R.ok(deviceService.myDevices(customerId));
    }

    private void verifyInternalToken(String internalToken) {
        if (internalServiceToken == null || internalServiceToken.isBlank()
                || internalToken == null || !internalServiceToken.equals(internalToken)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "内部接口令牌无效");
        }
    }
}
