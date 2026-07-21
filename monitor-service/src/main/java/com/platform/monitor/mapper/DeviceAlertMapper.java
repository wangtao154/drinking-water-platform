package com.platform.monitor.mapper;

import com.platform.common.base.BaseMapperPlus;
import com.platform.monitor.entity.DeviceAlert;
import com.platform.monitor.vo.AlertStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 设备告警 Mapper
 */
@Mapper
public interface DeviceAlertMapper extends BaseMapperPlus<DeviceAlert> {

    /**
     * 告警统计
     */
    @Select("SELECT COUNT(*) as totalAlerts, " +
            "SUM(CASE WHEN handled_status='UNHANDLED' THEN 1 ELSE 0 END) as unhandledAlerts, " +
            "SUM(CASE WHEN handled_status='HANDLED' THEN 1 ELSE 0 END) as handledAlerts, " +
            "SUM(CASE WHEN alert_level='WARNING' THEN 1 ELSE 0 END) as warningAlerts, " +
            "SUM(CASE WHEN alert_level='ALARM' THEN 1 ELSE 0 END) as alarmAlerts, " +
            "SUM(CASE WHEN push_status='PUSHED' THEN 1 ELSE 0 END) as pushedAlerts " +
            "FROM device_alert")
    AlertStatsVO selectAlertStats();
}
