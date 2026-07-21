package com.platform.push.mapper;

import com.platform.common.base.BaseMapperPlus;
import com.platform.push.entity.DeviceAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 设备告警 Mapper
 */
@Mapper
public interface DeviceAlertMapper extends BaseMapperPlus<DeviceAlert> {

    @Select("SELECT COUNT(*) FROM device_alert WHERE push_status='PUSHED'")
    Long countPushed();

    @Select("SELECT COUNT(*) FROM device_alert WHERE push_status='UNPUSHED'")
    Long countUnpushed();
}
