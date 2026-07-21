package com.platform.workorder.mapper;

import com.platform.common.base.BaseMapperPlus;
import com.platform.workorder.entity.WorkOrderLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单流转日志 Mapper
 */
@Mapper
public interface WorkOrderLogMapper extends BaseMapperPlus<WorkOrderLog> {
}
