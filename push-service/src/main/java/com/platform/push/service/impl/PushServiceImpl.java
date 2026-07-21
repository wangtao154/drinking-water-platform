package com.platform.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.dto.PageQueryDTO;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.PageResult;
import com.platform.common.result.ResultCode;
import com.platform.push.entity.DeviceAlert;
import com.platform.push.mapper.DeviceAlertMapper;
import com.platform.push.service.PushService;
import com.platform.push.vo.PushStatsVO;
import com.platform.push.vo.PushTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警推送 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private final DeviceAlertMapper deviceAlertMapper;

    @Override
    public PageResult<PushTaskVO> unpushedPage(PageQueryDTO query) {
        query.normalize();
        Page<DeviceAlert> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<DeviceAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceAlert::getPushStatus, "UNPUSHED")
                .orderByDesc(DeviceAlert::getTriggeredAt);

        Page<DeviceAlert> result = deviceAlertMapper.selectPage(page, wrapper);
        List<PushTaskVO> voList = result.getRecords().stream()
                .map(this::toPushTaskVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getSize(),
                result.getCurrent(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushAlert(Long alertId) {
        DeviceAlert alert = deviceAlertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "告警不存在");
        }
        if (!"UNPUSHED".equals(alert.getPushStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_CONFLICT, "告警已推送");
        }

        LambdaUpdateWrapper<DeviceAlert> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(DeviceAlert::getPushStatus, "PUSHED")
                .set(DeviceAlert::getPushedAt, LocalDateTime.now())
                .eq(DeviceAlert::getId, alertId);
        deviceAlertMapper.update(updateWrapper);

        log.info("[推送] 推送单条告警: alertId={}", alertId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushBatch() {
        LambdaQueryWrapper<DeviceAlert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceAlert::getPushStatus, "UNPUSHED");
        List<DeviceAlert> alerts = deviceAlertMapper.selectList(queryWrapper);

        if (alerts.isEmpty()) {
            log.info("[推送] 无待推送告警");
            return;
        }

        LambdaUpdateWrapper<DeviceAlert> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(DeviceAlert::getPushStatus, "PUSHED")
                .set(DeviceAlert::getPushedAt, LocalDateTime.now())
                .eq(DeviceAlert::getPushStatus, "UNPUSHED");
        deviceAlertMapper.update(updateWrapper);

        log.info("[推送] 批量推送告警: count={}", alerts.size());
    }

    @Override
    public PushStatsVO getStats() {
        PushStatsVO stats = new PushStatsVO();
        stats.setTotalPushed(deviceAlertMapper.countPushed());
        stats.setTotalUnpushed(deviceAlertMapper.countUnpushed());
        stats.setTotalFailed(0L);
        return stats;
    }

    /**
     * 实体转 PushTaskVO
     */
    private PushTaskVO toPushTaskVO(DeviceAlert entity) {
        PushTaskVO vo = new PushTaskVO();
        vo.setAlertId(entity.getId());
        vo.setDeviceId(entity.getDeviceId());
        vo.setAlertType(entity.getAlertType());
        vo.setAlertLevel(entity.getAlertLevel());
        vo.setAlertMessage(entity.getAlertMessage());
        vo.setTriggeredAt(entity.getTriggeredAt());
        vo.setPushStatus(entity.getPushStatus());
        return vo;
    }
}
