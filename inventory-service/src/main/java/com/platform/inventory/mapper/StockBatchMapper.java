package com.platform.inventory.mapper;

import com.platform.common.base.BaseMapperPlus;
import com.platform.inventory.entity.StockBatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存批次 Mapper
 */
@Mapper
public interface StockBatchMapper extends BaseMapperPlus<StockBatch> {
}
