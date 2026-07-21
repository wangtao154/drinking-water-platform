package com.platform.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.user.entity.Dealer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DealerMapper extends BaseMapper<Dealer> {

    default Dealer selectByDealerCode(String dealerCode) {
        return selectOne(new LambdaQueryWrapper<Dealer>()
                .eq(Dealer::getDealerCode, dealerCode));
    }
}
