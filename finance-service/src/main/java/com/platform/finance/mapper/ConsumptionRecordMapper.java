package com.platform.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.finance.entity.ConsumptionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ConsumptionRecordMapper extends BaseMapper<ConsumptionRecord> {

    @Select("SELECT " +
            "COALESCE(SUM(consume_amount), 0) AS totalConsumption, " +
            "COALESCE(SUM(revenue_amount), 0) AS totalRevenue " +
            "FROM consumption_record")
    Map<String, Object> selectConsumptionSummary();
}
