package com.platform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.user.dto.WorkerCreateDTO;
import com.platform.user.dto.WorkerQueryDTO;
import com.platform.user.dto.WorkerUpdateDTO;
import com.platform.user.entity.Worker;
import com.platform.user.mapper.WorkerMapper;
import com.platform.user.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final WorkerMapper workerMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Worker create(WorkerCreateDTO dto) {
        Worker worker = new Worker();
        worker.setName(dto.getName());
        worker.setPhone(dto.getPhone());
        worker.setDealerId(dto.getDealerId());
        worker.setPassword(passwordEncoder.encode(dto.getPassword()));
        worker.setPasswordChanged(false);
        worker.setStatus("ACTIVE");
        worker.setRegisteredAt(LocalDateTime.now());
        worker.setServiceCount(0);
        worker.setRating(new BigDecimal("5.00"));
        workerMapper.insert(worker);
        return worker;
    }

    @Override
    @Transactional
    public Worker update(Long id, WorkerUpdateDTO dto) {
        Worker worker = workerMapper.selectById(id);
        if (worker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "运维人员不存在");
        }
        if (StringUtils.hasText(dto.getName())) {
            worker.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            worker.setPhone(dto.getPhone());
        }
        if (dto.getDealerId() != null) {
            worker.setDealerId(dto.getDealerId());
        }
        workerMapper.updateById(worker);
        return worker;
    }

    @Override
    public Worker getById(Long id) {
        Worker worker = workerMapper.selectById(id);
        if (worker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "运维人员不存在");
        }
        return worker;
    }

    @Override
    public IPage<Worker> page(WorkerQueryDTO query) {
        query.normalize();
        LambdaQueryWrapper<Worker> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Worker::getName, query.getName());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(Worker::getPhone, query.getPhone());
        }
        if (query.getDealerId() != null) {
            wrapper.eq(Worker::getDealerId, query.getDealerId());
        }
        if (StringUtils.hasText(query.getOrderBy())) {
            wrapper.last("ORDER BY " + query.getOrderBy() + " " + query.getOrderDirection());
        } else {
            wrapper.orderByDesc(Worker::getCreatedAt);
        }
        Page<Worker> page = new Page<>(query.getPageNum(), query.getPageSize());
        return workerMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Worker worker = workerMapper.selectById(id);
        if (worker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "运维人员不存在");
        }
        workerMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        Worker worker = workerMapper.selectById(id);
        if (worker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "运维人员不存在");
        }
        // 重置密码为手机号后6位
        String phone = worker.getPhone();
        if (phone == null || phone.length() < 6) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "手机号格式不正确");
        }
        String defaultPassword = phone.substring(phone.length() - 6);
        worker.setPassword(passwordEncoder.encode(defaultPassword));
        worker.setPasswordChanged(false);

        // MyBatis-Plus updateById 会忽略 null，但 passwordChanged=false 不是 null，所以正常
        workerMapper.updateById(worker);
    }
}
