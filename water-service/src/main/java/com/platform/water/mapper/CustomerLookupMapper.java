package com.platform.water.mapper;

import com.platform.water.entity.CustomerLookup;
import org.apache.ibatis.annotations.Select;

public interface CustomerLookupMapper {

    @Select("SELECT id, open_id, status FROM customer WHERE id = #{id} AND deleted = 0 LIMIT 1")
    CustomerLookup selectById(Long id);
}
