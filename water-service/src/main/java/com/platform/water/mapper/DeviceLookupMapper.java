package com.platform.water.mapper;

import com.platform.water.entity.DeviceLookup;
import org.apache.ibatis.annotations.Select;

public interface DeviceLookupMapper {

    @Select("SELECT id, device_id, sn, online_status, lifecycle_status FROM device WHERE device_id = #{deviceId} AND deleted = 0 LIMIT 1")
    DeviceLookup selectByDeviceId(String deviceId);

    @Select("SELECT id, device_id, sn, online_status, lifecycle_status FROM device WHERE sn = #{sn} AND deleted = 0 LIMIT 1")
    DeviceLookup selectBySn(String sn);
}
