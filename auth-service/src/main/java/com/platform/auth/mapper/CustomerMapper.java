package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.auth.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户表 Mapper（auth-service 内部直接查询 customer 表）
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 根据微信 openId 查询客户
     */
    default Customer selectByOpenId(String openId) {
        return this.selectOne(
                Wrappers.<Customer>lambdaQuery()
                        .eq(Customer::getOpenId, openId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 根据手机号查询客户
     */
    default Customer selectByPhone(String phone) {
        return this.selectOne(
                Wrappers.<Customer>lambdaQuery()
                        .eq(Customer::getPhone, phone)
                        .last("LIMIT 1")
        );
    }
}
