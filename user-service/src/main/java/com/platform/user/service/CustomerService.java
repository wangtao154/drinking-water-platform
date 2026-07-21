package com.platform.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.user.dto.CustomerCreateDTO;
import com.platform.user.dto.CustomerQueryDTO;
import com.platform.user.dto.CustomerUpdateDTO;
import com.platform.user.entity.Customer;
import com.platform.user.vo.CustomerVO;

import java.util.List;

public interface CustomerService {

    Customer create(CustomerCreateDTO dto);

    Customer update(Long id, CustomerUpdateDTO dto);

    Customer getById(Long id);

    IPage<CustomerVO> page(CustomerQueryDTO query);

    /**
     * 根据客户名称模糊查询客户 ID 列表
     */
    List<Long> findIdsByName(String name);

    void delete(Long id);

    /**
     * 重置客户密码为手机号后6位
     */
    void resetPassword(Long id);
}
