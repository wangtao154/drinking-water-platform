package com.platform.device.controller;

import com.platform.common.result.R;
import com.platform.device.entity.DeviceModel;
import com.platform.device.service.DeviceModelService;
import com.platform.device.vo.DeviceModelVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-models")
@RequiredArgsConstructor
public class DeviceModelController {

    private final DeviceModelService deviceModelService;

    @GetMapping
    public R<List<DeviceModelVO>> list() {
        return R.ok(deviceModelService.list());
    }

    @PostMapping
    public R<DeviceModelVO> create(@RequestBody DeviceModel entity) {
        return R.ok(deviceModelService.create(entity));
    }

    @PutMapping("/{id}")
    public R<DeviceModelVO> update(@PathVariable Long id, @RequestBody DeviceModel entity) {
        entity.setId(id);
        return R.ok(deviceModelService.update(entity));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deviceModelService.delete(id);
        return R.ok();
    }
}
