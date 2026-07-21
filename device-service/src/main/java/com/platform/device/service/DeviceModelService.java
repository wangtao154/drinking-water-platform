package com.platform.device.service;

import com.platform.device.entity.DeviceModel;
import com.platform.device.vo.DeviceModelVO;

import java.util.List;

public interface DeviceModelService {

    List<DeviceModelVO> list();

    DeviceModelVO getById(Long id);

    DeviceModelVO create(DeviceModel entity);

    DeviceModelVO update(DeviceModel entity);

    void delete(Long id);
}
