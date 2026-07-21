package com.platform.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.base.BaseController;
import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.user.dto.SysUserCreateDTO;
import com.platform.user.dto.SysUserQueryDTO;
import com.platform.user.dto.SysUserUpdateDTO;
import com.platform.user.entity.SysUser;
import com.platform.user.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sys-users")
@RequiredArgsConstructor
public class SysUserController extends BaseController {

    private final SysUserService sysUserService;

    @PostMapping
    public R<SysUser> create(@Valid @RequestBody SysUserCreateDTO dto) {
        return success(sysUserService.create(dto));
    }

    @PutMapping("/{id}")
    public R<SysUser> update(@PathVariable Long id, @RequestBody SysUserUpdateDTO dto) {
        return success(sysUserService.update(id, dto));
    }

    @GetMapping("/{id}")
    public R<SysUser> getById(@PathVariable Long id) {
        return success(sysUserService.getById(id));
    }

    @GetMapping
    public R<PageResult<SysUser>> page(SysUserQueryDTO query) {
        IPage<SysUser> page = sysUserService.page(query);
        return pageResult(PageResult.of(page));
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        sysUserService.resetPassword(id);
        return success();
    }
}
