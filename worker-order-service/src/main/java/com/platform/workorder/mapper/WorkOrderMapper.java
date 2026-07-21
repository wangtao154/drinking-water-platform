package com.platform.workorder.mapper;

import com.platform.common.base.BaseMapperPlus;
import com.platform.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工单 Mapper
 */
@Mapper
public interface WorkOrderMapper extends BaseMapperPlus<WorkOrder> {

    /**
     * 统计指定运维人员当前进行中的工单数
     */
    @Select("SELECT COUNT(*) FROM work_order WHERE worker_id = #{workerId} AND order_status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') AND deleted = 0")
    int countActiveOrdersByWorker(@Param("workerId") Long workerId);
}
