package com.platform.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.finance.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
