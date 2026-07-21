package com.platform.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.dto.PageQueryDTO;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.monitor.dto.AlertHandleDTO;
import com.platform.monitor.dto.AlertPageQueryDTO;
import com.platform.monitor.entity.AlertThreshold;
import com.platform.monitor.entity.DeviceAlert;
import com.platform.monitor.mapper.AlertThresholdMapper;
import com.platform.monitor.mapper.DeviceAlertMapper;
import com.platform.monitor.service.MonitorService;
import com.platform.monitor.vo.AlertStatsVO;
import com.platform.monitor.vo.AlertVO;
import com.platform.monitor.vo.ThresholdVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备监控 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final DeviceAlertMapper deviceAlertMapper;
    private final AlertThresholdMapper alertThresholdMapper;

    @Override
    public PageResult<AlertVO> alertPage(AlertPageQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<DeviceAlert> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getDeviceId())) {
            wrapper.eq(DeviceAlert::getDeviceId, query.getDeviceId());
        }
        if (StringUtils.hasText(query.getAlertType())) {
            wrapper.eq(DeviceAlert::getAlertType, query.getAlertType());
        }
        if (StringUtils.hasText(query.getAlertLevel())) {
            wrapper.eq(DeviceAlert::getAlertLevel, query.getAlertLevel());
        }
        if (StringUtils.hasText(query.getHandledStatus())) {
            wrapper.eq(DeviceAlert::getHandledStatus, query.getHandledStatus());
        }
        if (StringUtils.hasText(query.getPushStatus())) {
            wrapper.eq(DeviceAlert::getPushStatus, query.getPushStatus());
        }
        wrapper.orderByDesc(DeviceAlert::getCreatedAt);

        Page<DeviceAlert> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<DeviceAlert> result = deviceAlertMapper.selectPage(page, wrapper);

        List<AlertVO> voList = result.getRecords().stream()
                .map(AlertVO::fromEntity)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getSize(),
                result.getCurrent(), voList);
    }

    @Override
    public AlertVO getAlertById(Long id) {
        DeviceAlert entity = getAlertEntityById(id);
        return AlertVO.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlert(Long id, AlertHandleDTO dto) {
        DeviceAlert entity = getAlertEntityById(id);
        entity.setHandledStatus("HANDLED");
        entity.setHandledAt(LocalDateTime.now());
        deviceAlertMapper.updateById(entity);
        log.info("[Monitor] 处理告警: id={}, handleRemark={}", id, dto.getHandleRemark());
    }

    @Override
    public AlertStatsVO getAlertStats() {
        AlertStatsVO stats = deviceAlertMapper.selectAlertStats();
        if (stats == null) {
            stats = new AlertStatsVO();
            stats.setTotalAlerts(0L);
            stats.setUnhandledAlerts(0L);
            stats.setHandledAlerts(0L);
            stats.setWarningAlerts(0L);
            stats.setAlarmAlerts(0L);
            stats.setPushedAlerts(0L);
        }
        return stats;
    }

    @Override
    public PageResult<ThresholdVO> thresholdPage(PageQueryDTO query) {
        query.normalize();

        LambdaQueryWrapper<AlertThreshold> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AlertThreshold::getThresholdKey);

        Page<AlertThreshold> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<AlertThreshold> result = alertThresholdMapper.selectPage(page, wrapper);

        List<ThresholdVO> voList = result.getRecords().stream()
                .map(ThresholdVO::fromEntity)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getSize(),
                result.getCurrent(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThresholdVO updateThreshold(Long id, String thresholdValue) {
        AlertThreshold entity = getThresholdEntityById(id);
        entity.setThresholdValue(thresholdValue);
        alertThresholdMapper.updateById(entity);
        log.info("[Monitor] 更新阈值: id={}, key={}, value={}", id, entity.getThresholdKey(), thresholdValue);
        return ThresholdVO.fromEntity(entity);
    }

    // ==================== 私有方法 ====================

    /**
     * 根据ID查询告警实体，不存在则抛异常
     */
    private DeviceAlert getAlertEntityById(Long id) {
        DeviceAlert entity = deviceAlertMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return entity;
    }

    /**
     * 根据ID查询阈值实体，不存在则抛异常
     */
    private AlertThreshold getThresholdEntityById(Long id) {
        AlertThreshold entity = alertThresholdMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return entity;
    }
}
