package com.platform.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.iot.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跨服务查 system-service 的 sys_config 表（共享 MySQL）
 */
@Mapper
public interface SysConfigLookupMapper extends BaseMapper<SysConfig> {
}
