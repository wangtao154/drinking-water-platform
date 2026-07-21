package com.platform.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.device.entity.DeviceModel;
import com.platform.device.mapper.DeviceModelMapper;
import com.platform.device.service.DeviceModelService;
import com.platform.device.vo.DeviceModelVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceModelServiceImpl implements DeviceModelService {

    private final DeviceModelMapper deviceModelMapper;

    @Override
    public List<DeviceModelVO> list() {
        List<DeviceModel> models = deviceModelMapper.selectList(
                new LambdaQueryWrapper<DeviceModel>()
                        .eq(DeviceModel::getStatus, "ENABLED")
        );
        return models.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public DeviceModelVO getById(Long id) {
        DeviceModel model = deviceModelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toVO(model);
    }

    @Override
    public DeviceModelVO create(DeviceModel entity) {
        deviceModelMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public DeviceModelVO update(DeviceModel entity) {
        DeviceModel existing = deviceModelMapper.selectById(entity.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        deviceModelMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public void delete(Long id) {
        DeviceModel existing = deviceModelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        deviceModelMapper.deleteById(id);
    }

    private DeviceModelVO toVO(DeviceModel entity) {
        DeviceModelVO vo = new DeviceModelVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
