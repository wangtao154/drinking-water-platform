package com.platform.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.iot.entity.Device;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DeviceLookupMapper extends BaseMapper<Device> {

    @Select("SELECT id, device_id, sn, model_id, online_status, lifecycle_status " +
            "FROM device WHERE sn = #{sn} AND deleted = 0")
    Device selectBySn(String sn);

    @Select("SELECT id, device_id, sn, model_id, online_status, lifecycle_status " +
            "FROM device WHERE device_id = #{deviceId} AND deleted = 0")
    Device selectByDeviceId(String deviceId);

    @Select("SELECT id, device_id, sn, model_id, online_status, lifecycle_status " +
            "FROM device WHERE deleted = 0 AND lifecycle_status <> 'RETURNED' " +
            "AND sn IS NOT NULL AND sn <> '' AND online_status = #{onlineStatus}")
    List<Device> selectHeartbeatCandidates(@Param("onlineStatus") int onlineStatus);

    @Update("UPDATE device SET updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND deleted = 0")
    int touchTelemetryReportedAt(@Param("id") Long id);
}
