package com.platform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.user.dto.CustomerCreateDTO;
import com.platform.user.dto.CustomerQueryDTO;
import com.platform.user.dto.CustomerUpdateDTO;
import com.platform.user.entity.Customer;
import com.platform.user.entity.Dealer;
import com.platform.user.mapper.CustomerMapper;
import com.platform.user.mapper.DealerMapper;
import com.platform.user.service.CustomerService;
import com.platform.user.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final DealerMapper dealerMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    @Transactional
    public Customer create(CustomerCreateDTO dto) {
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setCustomerType(dto.getCustomerType());
        customer.setIdCard(dto.getIdCard());
        customer.setOrgName(dto.getOrgName());
        customer.setDealerId(dto.getDealerId());
        customer.setStatus("ACTIVE");
        customer.setRegisteredAt(LocalDateTime.now());
        customer.setBalance(0L);
        customer.setRemainDuration(0);
        customer.setRemainFlow(0L);
        // 初始密码 = 手机号后6位，BCrypt 加密
        String initialPassword = dto.getPhone() != null && dto.getPhone().length() >= 6
                ? dto.getPhone().substring(dto.getPhone().length() - 6) : "123456";
        customer.setPassword(bCryptPasswordEncoder.encode(initialPassword));
        customer.setPasswordChanged(false);
        customerMapper.insert(customer);
        log.info("[Customer] 创建客户: id={}, phone={}, 初始密码=手机号后6位", customer.getId(), dto.getPhone());
        return customer;
    }

    @Override
    @Transactional
    public Customer update(Long id, CustomerUpdateDTO dto) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }
        if (StringUtils.hasText(dto.getName())) {
            customer.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getCustomerType())) {
            customer.setCustomerType(dto.getCustomerType());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            customer.setPhone(dto.getPhone());
        }
        if (dto.getIdCard() != null) {
            customer.setIdCard(dto.getIdCard());
        }
        if (dto.getOrgName() != null) {
            customer.setOrgName(dto.getOrgName());
        }
        if (dto.getAddress() != null) {
            customer.setAddress(dto.getAddress());
        }
        if (dto.getDealerId() != null) {
            customer.setDealerId(dto.getDealerId());
        }
        customerMapper.updateById(customer);
        return customer;
    }

    @Override
    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }
        return customer;
    }

    @Override
    public IPage<CustomerVO> page(CustomerQueryDTO query) {
        query.normalize();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Customer::getName, query.getName());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(Customer::getPhone, query.getPhone());
        }
        if (StringUtils.hasText(query.getCustomerType())) {
            wrapper.eq(Customer::getCustomerType, query.getCustomerType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Customer::getStatus, query.getStatus());
        }
        if (query.getDealerId() != null) {
            wrapper.eq(Customer::getDealerId, query.getDealerId());
        }
        if (StringUtils.hasText(query.getOrderBy())) {
            wrapper.last("ORDER BY " + query.getOrderBy() + " " + query.getOrderDirection());
        } else {
            wrapper.orderByDesc(Customer::getCreatedAt);
        }
        Page<Customer> page = new Page<>(query.getPageNum(), query.getPageSize());
        customerMapper.selectPage(page, wrapper);

        // 批量查询经销商名称
        Set<Long> dealerIds = new HashSet<>();
        for (Customer c : page.getRecords()) {
            if (c.getDealerId() != null) {
                dealerIds.add(c.getDealerId());
            }
        }
        Map<Long, String> dealerNameMap = new HashMap<>();
        if (!dealerIds.isEmpty()) {
            List<Dealer> dealers = dealerMapper.selectBatchIds(dealerIds);
            for (Dealer d : dealers) {
                dealerNameMap.put(d.getId(), d.getDealerName());
            }
        }

        // 转换为 VO
        IPage<CustomerVO> voPage = page.convert(c -> {
            CustomerVO vo = new CustomerVO();
            BeanUtils.copyProperties(c, vo);
            vo.setCustomerName(c.getName());
            vo.setEnterpriseName(c.getOrgName());
            if (c.getDealerId() != null) {
                vo.setDealerName(dealerNameMap.getOrDefault(c.getDealerId(), null));
            }
            return vo;
        });

        return voPage;
    }

    @Override
    public List<Long> findIdsByName(String name) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Customer::getName, name.trim());
        }
        wrapper.select(Customer::getId);
        return customerMapper.selectList(wrapper).stream()
                .map(Customer::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }
        customerMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }
        if (customer.getPhone() == null || customer.getPhone().length() < 6) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "客户手机号不完整，无法重置密码");
        }
        // 重置密码 = 手机号后6位
        String initialPassword = customer.getPhone().substring(customer.getPhone().length() - 6);
        customer.setPassword(bCryptPasswordEncoder.encode(initialPassword));
        customer.setPasswordChanged(false);
        customerMapper.updateById(customer);
        log.info("[Customer] 重置客户密码: id={}, phone={}", id, customer.getPhone());
    }
}
