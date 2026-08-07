package com.platform.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.auth.CurrentUser;
import com.platform.common.auth.UserContext;
import com.platform.common.dto.DeviceActivatedEvent;
import com.platform.common.enums.DeviceLifecycleStatus;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.common.util.QRCodeUtil;
import com.platform.device.dto.DeviceBindDTO;
import com.platform.device.dto.DeviceQueryDTO;
import com.platform.device.dto.DeviceRegisterDTO;
import com.platform.device.dto.DeviceUpdateDTO;
import com.platform.device.dto.feign.CustomerFeignDTO;
import com.platform.device.entity.Device;
import com.platform.device.entity.DeviceBinding;
import com.platform.device.entity.DeviceModel;
import com.platform.device.entity.DeviceStatusLog;
import com.platform.device.feign.UserServiceClient;
import com.platform.device.mapper.DeviceBindingMapper;
import com.platform.device.mapper.DeviceMapper;
import com.platform.device.mapper.DeviceModelMapper;
import com.platform.device.mapper.DeviceStatusLogMapper;
import com.platform.device.service.DeviceService;
import com.platform.device.vo.DeviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceModelMapper deviceModelMapper;
    private final DeviceBindingMapper deviceBindingMapper;
    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final UserServiceClient userServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public DeviceVO register(DeviceRegisterDTO dto) {
        validateUniqueForRegister(dto);

        Device device = new Device();
        BeanUtils.copyProperties(dto, device);
        device.setLifecycleStatus(DeviceLifecycleStatus.REGISTERED.name());
        device.setOnlineStatus(0);

        // 生成二维码（Base64）：内容为「扫普通链接二维码打开小程序」的 URL
        // 微信扫码 → 跳转到小程序 pages/water-scan/water-scan
        // 路径带 deviceId 参数，旧版小程序也能通过 onLoad 的 deviceId 参数识别
        String qrContent = "https://zyswx.juconyun.com/wxwater/?deviceId=" + dto.getDeviceId();
        String qrCodeBase64 = QRCodeUtil.generateBase64(qrContent);
        device.setQrCodeUrl(qrCodeBase64);

        deviceMapper.insert(device);

        // 记录状态变更日志
        logStatusChange(dto.getDeviceId(), null, DeviceLifecycleStatus.REGISTERED.name(), "设备登记");

        return toVO(device);
    }

    private void validateUniqueForRegister(DeviceRegisterDTO dto) {
        Integer deviceIdCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM device WHERE device_id = ? AND deleted = 0",
                Integer.class,
                dto.getDeviceId());
        if (deviceIdCount != null && deviceIdCount > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "设备ID已存在");
        }

        Integer snCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM device WHERE sn = ? AND deleted = 0",
                Integer.class,
                dto.getSn());
        if (snCount != null && snCount > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "网关序列号已存在");
        }
    }

    @Override
    @Transactional
    public DeviceVO activate(String deviceId) {
        Device device = getDeviceByDeviceIdOrThrow(deviceId);

        if (!DeviceLifecycleStatus.REGISTERED.name().equals(device.getLifecycleStatus())) {
            throw new BusinessException(ResultCode.DEVICE_STATUS_CONFLICT);
        }

        String fromStatus = device.getLifecycleStatus();
        device.setLifecycleStatus(DeviceLifecycleStatus.PENDING_INSTALL.name());
        deviceMapper.updateById(device);

        // 记录状态变更日志
        logStatusChange(deviceId, fromStatus, device.getLifecycleStatus(), "设备激活");

        // 发布 RabbitMQ 事件
        DeviceActivatedEvent event = DeviceActivatedEvent.builder()
                .sn(device.getSn())
                .deviceId(device.getDeviceId())
                .customerId(device.getCustomerId())
                .activatedAt(System.currentTimeMillis())
                .build();
        rabbitTemplate.convertAndSend("device.exchange", "device.activated", event);
        log.info("[DeviceService] 发布设备激活事件: {}", event);

        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO bind(DeviceBindDTO dto, Long customerId) {
        Device device = getDeviceByDeviceIdOrThrow(dto.getDeviceId());

        // 检查设备是否已被绑定
        LambdaQueryWrapper<DeviceBinding> bindingQuery = new LambdaQueryWrapper<>();
        bindingQuery.eq(DeviceBinding::getDeviceId, dto.getDeviceId())
                .eq(DeviceBinding::getStatus, "ACTIVE");
        DeviceBinding activeBinding = deviceBindingMapper.selectOne(bindingQuery);
        if (activeBinding != null) {
            throw new BusinessException(ResultCode.DEVICE_ALREADY_BOUND);
        }

        // 使用传入的 customerId 或从 DTO 中获取
        Long bindCustomerId = customerId != null ? customerId : dto.getCustomerId();
        if (bindCustomerId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }

        String fromStatus = device.getLifecycleStatus();
        device.setLifecycleStatus(DeviceLifecycleStatus.ACTIVATED_ONLINE.name());
        device.setCustomerId(bindCustomerId);
        device.setActivatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);

        // 记录状态变更日志
        logStatusChange(dto.getDeviceId(), fromStatus, device.getLifecycleStatus(), "设备绑定客户");

        // 创建绑定记录
        DeviceBinding binding = new DeviceBinding();
        binding.setDeviceId(dto.getDeviceId());
        binding.setCustomerId(bindCustomerId);
        binding.setBindType("INPUT_ID");
        binding.setBindAt(LocalDateTime.now());
        binding.setStatus("ACTIVE");
        deviceBindingMapper.insert(binding);

        // 发布 RabbitMQ 事件
        DeviceActivatedEvent event = DeviceActivatedEvent.builder()
                .sn(device.getSn())
                .deviceId(device.getDeviceId())
                .customerId(bindCustomerId)
                .activatedAt(System.currentTimeMillis())
                .build();
        rabbitTemplate.convertAndSend("device.exchange", "device.activated", event);
        log.info("[DeviceService] 发布设备激活事件（绑定）: {}", event);

        return toVO(device);
    }

    @Override
    @Transactional
    public void unbind(String deviceId) {
        Device device = getDeviceByDeviceIdOrThrow(deviceId);

        // 更新绑定记录状态
        LambdaQueryWrapper<DeviceBinding> bindingQuery = new LambdaQueryWrapper<>();
        bindingQuery.eq(DeviceBinding::getDeviceId, deviceId)
                .eq(DeviceBinding::getStatus, "ACTIVE");
        DeviceBinding binding = deviceBindingMapper.selectOne(bindingQuery);
        if (binding != null) {
            binding.setStatus("UNBOUND");
            binding.setUnbindAt(LocalDateTime.now());
            deviceBindingMapper.updateById(binding);
        }

        // 更新设备状态为 RETURNED，清除客户关联和激活时间
        String fromStatus = device.getLifecycleStatus();
        LambdaUpdateWrapper<Device> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Device::getDeviceId, deviceId)
                .set(Device::getLifecycleStatus, DeviceLifecycleStatus.RETURNED.name())
                .set(Device::getCustomerId, null)
                .set(Device::getActivatedAt, null)
                .set(Device::getReturnedAt, LocalDateTime.now());
        deviceMapper.update(null, updateWrapper);

        // 记录状态变更日志
        logStatusChange(deviceId, fromStatus, DeviceLifecycleStatus.RETURNED.name(), "设备解绑");
    }

    @Override
    public DeviceVO getDetail(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        return toVO(device);
    }

    @Override
    public DeviceVO getDetailByIdOrDeviceId(String id) {
        if (id == null || id.isEmpty()) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        // 如果 id 是纯数字（雪花 ID 最多 19 位），按数据库主键查询；否则按业务 deviceId 查询
        Device device;
        if (id.matches("\\d{1,19}")) {
            try {
                Long pk = Long.parseLong(id);
                device = deviceMapper.selectById(pk);
                if (device != null) {
                    return toVO(device);
                }
                // 主键查不到时降级为业务 ID 查询（兼容数字型业务 ID）
                device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
                );
            } catch (NumberFormatException e) {
                device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
                );
            }
        } else {
            device = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
            );
        }
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        return toVO(device);
    }

    @Override
    public PageResult<DeviceVO> page(DeviceQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Device::getDeviceId, kw).or().like(Device::getSn, kw));
        }
        if (StringUtils.hasText(query.getDeviceId())) {
            wrapper.like(Device::getDeviceId, query.getDeviceId().trim());
        }
        if (StringUtils.hasText(query.getSn())) {
            wrapper.like(Device::getSn, query.getSn().trim());
        }
        if (StringUtils.hasText(query.getLifecycleStatus())) {
            wrapper.eq(Device::getLifecycleStatus, query.getLifecycleStatus());
        }
        if (query.getModelId() != null) {
            wrapper.eq(Device::getModelId, query.getModelId());
        }
        if (query.getDealerId() != null) {
            wrapper.eq(Device::getDealerId, query.getDealerId());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(Device::getCustomerId, query.getCustomerId());
        }
        if (query.getOnlineStatus() != null) {
            wrapper.eq(Device::getOnlineStatus, query.getOnlineStatus());
        }

        // 型号名称搜索：查询 device_model 表获取匹配的 modelId 列表
        if (StringUtils.hasText(query.getModelName())) {
            String modelName = query.getModelName().trim();
            List<DeviceModel> models = deviceModelMapper.selectList(
                    new LambdaQueryWrapper<DeviceModel>().like(DeviceModel::getModelName, modelName)
            );
            if (models.isEmpty()) {
                // 没有匹配的型号，直接返回空结果
                return PageResult.of(new Page<>(query.getPageNum(), query.getPageSize()));
            }
            List<Long> modelIds = models.stream().map(DeviceModel::getId).collect(Collectors.toList());
            wrapper.in(Device::getModelId, modelIds);
        }

        // 绑定客户名称搜索：通过 Feign 调用 user-service 获取 customer IDs
        if (StringUtils.hasText(query.getCustomerName())) {
            String customerName = query.getCustomerName().trim();
            try {
                com.platform.common.result.R<List<Long>> r = userServiceClient.findCustomerIdsByName(customerName);
                if (r != null && r.getCode() != null && r.getCode() == 200 && r.getData() != null) {
                    List<Long> customerIds = r.getData();
                    if (customerIds.isEmpty()) {
                        return PageResult.of(new Page<>(query.getPageNum(), query.getPageSize()));
                    }
                    wrapper.in(Device::getCustomerId, customerIds);
                }
            } catch (Exception e) {
                log.warn("[DeviceService] 按客户名称查询失败, customerName={}: {}", customerName, e.getMessage());
            }
        }

        // 激活时间范围搜索
        if (query.getActivatedAtStart() != null) {
            wrapper.ge(Device::getActivatedAt, query.getActivatedAtStart());
        }
        if (query.getActivatedAtEnd() != null) {
            wrapper.le(Device::getActivatedAt, query.getActivatedAtEnd());
        }

        // 入库时间范围搜索
        if (query.getCreatedAtStart() != null) {
            wrapper.ge(Device::getCreatedAt, query.getCreatedAtStart());
        }
        if (query.getCreatedAtEnd() != null) {
            wrapper.le(Device::getCreatedAt, query.getCreatedAtEnd());
        }

        wrapper.orderByDesc(Device::getCreatedAt);

        IPage<Device> page = new Page<>(query.getPageNum(), query.getPageSize());
        deviceMapper.selectPage(page, wrapper);

        return PageResult.of(page.convert(this::toVO));
    }

    @Override
    public DeviceVO getDeviceByDeviceId(String deviceId) {
        Device device = getDeviceByDeviceIdOrThrow(deviceId);
        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO regenerateQrCode(String deviceId) {
        Device device = getDeviceByDeviceIdOrThrow(deviceId);
        // 新二维码内容：URL 带查询参数，旧版小程序也能通过 options.deviceId 识别
        String qrContent = "https://zyswx.juconyun.com/wxwater/?deviceId=" + device.getDeviceId();
        String qrCodeBase64 = QRCodeUtil.generateBase64(qrContent);
        device.setQrCodeUrl(qrCodeBase64);
        deviceMapper.updateById(device);
        log.info("[Device] 重新生成设备二维码: deviceId={}, content={}", deviceId, qrContent);
        return toVO(device);
    }

    @Override
    public int regenerateAllQrCodes() {
        List<Device> allDevices = deviceMapper.selectList(null);
        int count = 0;
        for (Device device : allDevices) {
            String qrContent = "https://zyswx.juconyun.com/wxwater/?deviceId=" + device.getDeviceId();
            String qrCodeBase64 = QRCodeUtil.generateBase64(qrContent);
            device.setQrCodeUrl(qrCodeBase64);
            deviceMapper.updateById(device);
            count++;
        }
        log.info("[Device] 批量重新生成设备二维码: 总数={}", count);
        return count;
    }

    @Override
    @Transactional
    public DeviceVO update(Long id, DeviceUpdateDTO dto) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        applyUpdate(device, dto);
        deviceMapper.updateById(device);
        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO updateByIdOrDeviceId(String id, DeviceUpdateDTO dto) {
        if (id == null || id.isEmpty()) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        Device device;
        if (id.matches("\\d{1,19}")) {
            try {
                Long pk = Long.parseLong(id);
                device = deviceMapper.selectById(pk);
                if (device != null) {
                    applyUpdate(device, dto);
                    deviceMapper.updateById(device);
                    return toVO(device);
                }
                device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
                );
            } catch (NumberFormatException e) {
                device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
                );
            }
        } else {
            device = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
            );
        }
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        applyUpdate(device, dto);
        deviceMapper.updateById(device);
        return toVO(device);
    }

    private void applyUpdate(Device device, DeviceUpdateDTO dto) {
        if (dto.getModelId() != null) {
            device.setModelId(dto.getModelId());
        }
        if (dto.getSn() != null && !dto.getSn().equals(device.getSn())) {
            // SN 发生变化，检查唯一性
            Device existing = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>().eq(Device::getSn, dto.getSn())
            );
            if (existing != null) {
                throw new BusinessException(40901, "网关序列号已存在");
            }
            device.setSn(dto.getSn());
        }
        if (dto.getIccid() != null) {
            device.setIccid(dto.getIccid());
        }
        if (dto.getImei() != null) {
            device.setImei(dto.getImei());
        }
        if (dto.getRemark() != null) {
            device.setRemark(dto.getRemark());
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        // 检查是否有未解绑的绑定记录
        Long activeBinding = deviceBindingMapper.selectCount(
                new LambdaQueryWrapper<DeviceBinding>()
                        .eq(DeviceBinding::getDeviceId, device.getDeviceId())
                        .eq(DeviceBinding::getStatus, "ACTIVE")
        );
        if (activeBinding != null && activeBinding > 0) {
            throw new BusinessException(ResultCode.DEVICE_STATUS_CONFLICT, "设备存在有效绑定，请先解绑后再删除");
        }
        // 逻辑删除
        deviceMapper.deleteById(id);
        log.info("[DeviceService] 逻辑删除设备: id={}, deviceId={}", id, device.getDeviceId());
    }

    @Override
    @Transactional
    public void deleteByIdOrDeviceId(String id) {
        if (id == null || id.isEmpty()) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        Device device;
        if (id.matches("\\d{1,19}")) {
            try {
                Long pk = Long.parseLong(id);
                device = deviceMapper.selectById(pk);
                if (device != null) {
                    delete(pk);
                    return;
                }
                device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
                );
            } catch (NumberFormatException e) {
                device = null;
            }
        } else {
            device = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, id)
            );
        }
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        delete(device.getId());
    }

    @Override
    public List<DeviceVO> myDevices(Long customerId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getCustomerId, customerId);
        wrapper.orderByDesc(Device::getCreatedAt);
        List<Device> devices = deviceMapper.selectList(wrapper);
        return devices.stream().map(this::toVO).collect(Collectors.toList());
    }

    private Device getDeviceByDeviceIdOrThrow(String deviceId) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, deviceId)
        );
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        return device;
    }

    private void logStatusChange(String deviceId, String fromStatus, String toStatus, String remark) {
        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(deviceId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);

        CurrentUser currentUser = UserContext.get();
        if (currentUser != null) {
            log.setOperatorId(currentUser.getUserId());
            log.setOperatorName(currentUser.getUserName());
        }

        log.setRemark(remark);
        deviceStatusLogMapper.insert(log);
    }

    private DeviceVO toVO(Device device) {
        DeviceVO vo = new DeviceVO();
        BeanUtils.copyProperties(device, vo);

        // 获取型号名称
        if (device.getModelId() != null) {
            DeviceModel model = deviceModelMapper.selectById(device.getModelId());
            if (model != null) {
                vo.setModelName(model.getModelName());
            }
        }

        // 获取客户名称
        if (device.getCustomerId() != null) {
            try {
                com.platform.common.result.R<CustomerFeignDTO> r = userServiceClient.getCustomerById(device.getCustomerId());
                if (r != null && r.getCode() != null && r.getCode() == 200 && r.getData() != null) {
                    vo.setCustomerName(r.getData().getName());
                }
            } catch (Exception e) {
                log.warn("[DeviceService] 获取客户名称失败, customerId={}: {}", device.getCustomerId(), e.getMessage());
            }
        }

        return vo;
    }
}
