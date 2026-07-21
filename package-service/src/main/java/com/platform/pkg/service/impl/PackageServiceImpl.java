package com.platform.pkg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.common.util.SnowflakeIdUtil;
import com.platform.pkg.dto.PackageCreateDTO;
import com.platform.pkg.dto.PackagePageQueryDTO;
import com.platform.pkg.dto.PackageUpdateDTO;
import com.platform.pkg.entity.WaterPackage;
import com.platform.pkg.mapper.PackageMapper;
import com.platform.pkg.service.PackageService;
import com.platform.pkg.vo.PackageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 套餐管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final PackageMapper packageMapper;
    private final SnowflakeIdUtil snowflakeIdUtil;

    /**
     * 序号生成器（每天重置）
     */
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static volatile String LAST_DATE = "";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageVO create(PackageCreateDTO dto) {
        WaterPackage entity = new WaterPackage();
        entity.setPackageName(dto.getPackageName());
        entity.setPackageType(dto.getPackageType());
        entity.setPrice(dto.getPrice());
        entity.setFaceValue(dto.getFaceValue());
        entity.setFlowQuota(dto.getFlowQuota());
        entity.setDurationDays(dto.getDurationDays());
        entity.setChargeMode(dto.getChargeMode());
        entity.setModelId(dto.getModelId());
        entity.setPhotoUrl(dto.getPhotoUrl());
        entity.setCommissionAmount(dto.getCommissionAmount());
        entity.setCommissionRate(dto.getCommissionRate());
        entity.setColdWaterRate(dto.getColdWaterRate());
        entity.setHotWaterRate(dto.getHotWaterRate());
        entity.setId(snowflakeIdUtil.nextId());
        entity.setPackageCode(generatePackageCode());
        entity.setStatus("ENABLED");
        packageMapper.insert(entity);
        log.info("[套餐] 创建套餐: id={}, code={}", entity.getId(), entity.getPackageCode());
        return PackageVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageVO update(Long id, PackageUpdateDTO dto) {
        WaterPackage entity = getEntityById(id);
        // 逐字段更新非空值
        if (dto.getPackageName() != null) {
            entity.setPackageName(dto.getPackageName());
        }
        if (dto.getPackageType() != null) {
            entity.setPackageType(dto.getPackageType());
        }
        if (dto.getPrice() != null) {
            entity.setPrice(dto.getPrice());
        }
        if (dto.getFaceValue() != null) {
            entity.setFaceValue(dto.getFaceValue());
        }
        if (dto.getFlowQuota() != null) {
            entity.setFlowQuota(dto.getFlowQuota());
        }
        if (dto.getDurationDays() != null) {
            entity.setDurationDays(dto.getDurationDays());
        }
        if (dto.getChargeMode() != null) {
            entity.setChargeMode(dto.getChargeMode());
        }
        if (dto.getModelId() != null) {
            entity.setModelId(dto.getModelId());
        }
        if (dto.getPhotoUrl() != null) {
            entity.setPhotoUrl(dto.getPhotoUrl());
        }
        if (dto.getCommissionAmount() != null) {
            entity.setCommissionAmount(dto.getCommissionAmount());
        }
        if (dto.getCommissionRate() != null) {
            entity.setCommissionRate(dto.getCommissionRate());
        }
        if (dto.getColdWaterRate() != null) {
            entity.setColdWaterRate(dto.getColdWaterRate());
        }
        if (dto.getHotWaterRate() != null) {
            entity.setHotWaterRate(dto.getHotWaterRate());
        }
        packageMapper.updateById(entity);
        log.info("[套餐] 更新套餐: id={}", id);
        return PackageVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WaterPackage entity = getEntityById(id);
        packageMapper.deleteById(id);
        log.info("[套餐] 删除套餐: id={}, code={}", id, entity.getPackageCode());
    }

    @Override
    public PackageVO getById(Long id) {
        WaterPackage entity = getEntityById(id);
        return PackageVO.fromEntity(entity);
    }

    @Override
    public PageResult<PackageVO> page(PackagePageQueryDTO dto) {
        dto.normalize();
        Page<WaterPackage> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<WaterPackage> wrapper = new LambdaQueryWrapper<>();
        if (dto.getPackageType() != null && !dto.getPackageType().isEmpty()) {
            wrapper.eq(WaterPackage::getPackageType, dto.getPackageType());
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            wrapper.eq(WaterPackage::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(WaterPackage::getCreatedAt);

        Page<WaterPackage> result = packageMapper.selectPage(page, wrapper);
        List<PackageVO> voList = result.getRecords().stream()
                .map(PackageVO::fromEntity)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getSize(),
                result.getCurrent(), voList);
    }

    @Override
    public List<PackageVO> listActive() {
        LambdaQueryWrapper<WaterPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterPackage::getStatus, "ENABLED")
                .orderByAsc(WaterPackage::getPrice);
        List<WaterPackage> list = packageMapper.selectList(wrapper);
        return list.stream()
                .map(PackageVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        WaterPackage entity = getEntityById(id);
        entity.setStatus("ENABLED");
        packageMapper.updateById(entity);
        log.info("[套餐] 上架套餐: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        WaterPackage entity = getEntityById(id);
        entity.setStatus("DISABLED");
        packageMapper.updateById(entity);
        log.info("[套餐] 下架套餐: id={}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 根据ID查询实体，不存在则抛异常
     */
    private WaterPackage getEntityById(Long id) {
        WaterPackage entity = packageMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.PACKAGE_NOT_FOUND);
        }
        return entity;
    }

    /**
     * 生成套餐编号：PKG + yyyyMMdd + 4位序号
     */
    private String generatePackageCode() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        synchronized (PackageServiceImpl.class) {
            if (!today.equals(LAST_DATE)) {
                LAST_DATE = today;
                SEQUENCE.set(0);
            }
            int seq = SEQUENCE.incrementAndGet();
            return "PKG" + today + String.format("%04d", seq);
        }
    }
}
