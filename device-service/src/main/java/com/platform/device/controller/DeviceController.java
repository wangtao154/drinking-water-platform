package com.platform.device.controller;

import com.platform.common.auth.UserContext;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.device.dto.DeviceBindDTO;
import com.platform.device.dto.DeviceQueryDTO;
import com.platform.device.dto.DeviceRegisterDTO;
import com.platform.device.dto.DeviceUpdateDTO;
import com.platform.device.service.DeviceService;
import com.platform.device.vo.DeviceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

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

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        deviceService.deleteByIdOrDeviceId(id);
        return R.ok();
    }

    @GetMapping
    public R<PageResult<DeviceVO>> page(DeviceQueryDTO query) {
        return R.ok(deviceService.page(query));
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

    /**
     * C 端：查询当前客户的所有设备
     */
    @GetMapping("/my")
    public R<List<DeviceVO>> myDevices() {
        Long customerId = UserContext.getUserId();
        return R.ok(deviceService.myDevices(customerId));
    }
}
