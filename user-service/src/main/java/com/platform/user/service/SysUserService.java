package com.platform.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.user.dto.SysUserCreateDTO;
import com.platform.user.dto.SysUserQueryDTO;
import com.platform.user.dto.SysUserUpdateDTO;
import com.platform.user.entity.SysUser;

public interface SysUserService {

    SysUser create(SysUserCreateDTO dto);

    SysUser update(Long id, SysUserUpdateDTO dto);

    SysUser getById(Long id);

    IPage<SysUser> page(SysUserQueryDTO query);

    void resetPassword(Long id);
}
