package com.platform.monitor.service;

import com.platform.common.dto.PageQueryDTO;
import com.platform.common.result.PageResult;
import com.platform.monitor.dto.AlertHandleDTO;
import com.platform.monitor.dto.AlertPageQueryDTO;
import com.platform.monitor.vo.AlertStatsVO;
import com.platform.monitor.vo.AlertVO;
import com.platform.monitor.vo.ThresholdVO;

import java.time.LocalDateTime;

/**
 * 设备监控 Service
 */
public interface MonitorService {

    /**
     * 分页查询告警
     */
    PageResult<AlertVO> alertPage(AlertPageQueryDTO query);

    /**
     * 根据ID获取告警详情
     */
    AlertVO getAlertById(Long id);

    /**
     * 处理告警
     */
    void handleAlert(Long id, AlertHandleDTO dto);

    /**
     * 告警统计
     */
    AlertStatsVO getAlertStats();

    /**
     * 按告警触发时间统计。开始、结束时间同时为空时返回全部统计。
     */
    AlertStatsVO getAlertStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查询阈值
     */
    PageResult<ThresholdVO> thresholdPage(PageQueryDTO query);

    /**
     * 更新阈值
     */
    ThresholdVO updateThreshold(Long id, String thresholdValue);
}
