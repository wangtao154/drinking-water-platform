package com.platform.device.service;

import com.platform.common.result.PageResult;
import com.platform.device.dto.DeviceBindDTO;
import com.platform.device.dto.DeviceQueryDTO;
import com.platform.device.dto.DeviceRegisterDTO;
import com.platform.device.dto.DeviceUpdateDTO;
import com.platform.device.vo.DeviceVO;

import java.util.List;

public interface DeviceService {

    DeviceVO register(DeviceRegisterDTO dto);

    DeviceVO activate(String deviceId);

    DeviceVO bind(DeviceBindDTO dto, Long customerId);

    void unbind(String deviceId);

    DeviceVO getDetail(Long id);

    /**
     * 查询设备详情：自动判断 id 是数据库主键（纯数字）还是业务 deviceId 字符串
     */
    DeviceVO getDetailByIdOrDeviceId(String id);

    PageResult<DeviceVO> page(DeviceQueryDTO query);

    DeviceVO getDeviceByDeviceId(String deviceId);

    /**
     * 重新生成单个设备的二维码
     */
    DeviceVO regenerateQrCode(String deviceId);

    /**
     * 批量重新生成所有设备的二维码
     */
    int regenerateAllQrCodes();

    DeviceVO update(Long id, DeviceUpdateDTO dto);

    /**
     * 更新设备：自动判断 id 是主键还是业务 deviceId
     */
    DeviceVO updateByIdOrDeviceId(String id, DeviceUpdateDTO dto);

    /**
     * 逻辑删除设备
     */
    void delete(Long id);

    /**
     * 逻辑删除设备：自动判断 id 是主键还是业务 deviceId
     */
    void deleteByIdOrDeviceId(String id);

    /**
     * C 端：查询当前客户的所有设备
     */
    List<DeviceVO> myDevices(Long customerId);
}
