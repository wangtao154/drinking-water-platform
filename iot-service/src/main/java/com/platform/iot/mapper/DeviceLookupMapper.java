package com.platform.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.iot.entity.Device;
import org.apache.ibatis.annotations.Select;

public interface DeviceLookupMapper extends BaseMapper<Device> {

    @Select("SELECT id, device_id, sn, model_id, online_status, lifecycle_status " +
            "FROM device WHERE sn = #{sn} AND deleted = 0")
    Device selectBySn(String sn);
}
