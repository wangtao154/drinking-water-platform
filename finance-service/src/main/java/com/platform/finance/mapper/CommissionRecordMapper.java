package com.platform.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.finance.entity.CommissionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface CommissionRecordMapper extends BaseMapper<CommissionRecord> {

    @Select("SELECT " +
            "COALESCE(SUM(commission_amount), 0) AS totalCommission, " +
            "COALESCE(SUM(CASE WHEN status = 'SETTLED' THEN commission_amount ELSE 0 END), 0) AS settledCommission, " +
            "COALESCE(SUM(CASE WHEN status = 'PENDING' THEN commission_amount ELSE 0 END), 0) AS pendingCommission " +
            "FROM commission_record")
    Map<String, Object> selectCommissionSummary();
}
