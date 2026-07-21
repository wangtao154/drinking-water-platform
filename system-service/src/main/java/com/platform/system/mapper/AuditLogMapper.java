package com.platform.system.mapper;

import com.platform.common.base.BaseMapperPlus;
import com.platform.system.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapperPlus<AuditLog> {
}
