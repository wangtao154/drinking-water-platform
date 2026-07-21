package com.platform.pkg.controller;

import com.platform.common.result.PageResult;
import com.platform.common.result.R;
import com.platform.pkg.dto.PackageCreateDTO;
import com.platform.pkg.dto.PackagePageQueryDTO;
import com.platform.pkg.dto.PackageUpdateDTO;
import com.platform.pkg.service.PackageService;
import com.platform.pkg.vo.PackageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理 Controller
 */
@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    /**
     * 创建套餐
     */
    @PostMapping
    public R<PackageVO> create(@Valid @RequestBody PackageCreateDTO dto) {
        return R.ok(packageService.create(dto));
    }

    /**
     * 更新套餐
     */
    @PutMapping("/{id}")
    public R<PackageVO> update(@PathVariable Long id, @Valid @RequestBody PackageUpdateDTO dto) {
        return R.ok(packageService.update(id, dto));
    }

    /**
     * 删除套餐
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        packageService.delete(id);
        return R.ok();
    }

    /**
     * 根据ID获取套餐
     */
    @GetMapping("/{id}")
    public R<PackageVO> getById(@PathVariable Long id) {
        return R.ok(packageService.getById(id));
    }

    /**
     * 分页查询套餐
     */
    @GetMapping
    public R<PageResult<PackageVO>> page(PackagePageQueryDTO dto) {
        return R.ok(packageService.page(dto));
    }

    /**
     * 获取全部在售套餐
     */
    @GetMapping("/active")
    public R<List<PackageVO>> listActive() {
        return R.ok(packageService.listActive());
    }

    /**
     * 上架套餐
     */
    @PutMapping("/{id}/enable")
    public R<Void> enable(@PathVariable Long id) {
        packageService.enable(id);
        return R.ok();
    }

    /**
     * 下架套餐
     */
    @PutMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        packageService.disable(id);
        return R.ok();
    }
}
