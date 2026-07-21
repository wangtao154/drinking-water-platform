package com.platform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.ResultCode;
import com.platform.user.dto.SysUserCreateDTO;
import com.platform.user.dto.SysUserQueryDTO;
import com.platform.user.dto.SysUserUpdateDTO;
import com.platform.user.entity.SysUser;
import com.platform.user.mapper.SysUserMapper;
import com.platform.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${system.default-password:123456}")
    private String defaultPassword;

    @Override
    @Transactional
    public SysUser create(SysUserCreateDTO dto) {
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(dto.getRoleId());
        user.setDepartment(dto.getDepartment());
        user.setEmployeeNo(dto.getEmployeeNo());
        user.setStatus("ENABLED");
        sysUserMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public SysUser update(Long id, SysUserUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getName())) {
            user.setName(dto.getName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getRoleId() != null) {
            user.setRoleId(dto.getRoleId());
        }
        if (StringUtils.hasText(dto.getDepartment())) {
            user.setDepartment(dto.getDepartment());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            user.setStatus(dto.getStatus());
        }
        sysUserMapper.updateById(user);
        return user;
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return user;
    }

    @Override
    public IPage<SysUser> page(SysUserQueryDTO query) {
        query.normalize();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(SysUser::getName, query.getName());
        }
        if (query.getRoleId() != null) {
            wrapper.eq(SysUser::getRoleId, query.getRoleId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getOrderBy())) {
            wrapper.last("ORDER BY " + query.getOrderBy() + " " + query.getOrderDirection());
        } else {
            wrapper.orderByDesc(SysUser::getCreatedAt);
        }
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        return sysUserMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(defaultPassword));
        sysUserMapper.updateById(user);
        log.info("系统用户密码已重置: userId={}", id);
    }
}
