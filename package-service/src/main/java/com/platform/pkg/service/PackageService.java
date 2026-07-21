package com.platform.pkg.service;

import com.platform.common.result.PageResult;
import com.platform.pkg.dto.PackageCreateDTO;
import com.platform.pkg.dto.PackagePageQueryDTO;
import com.platform.pkg.dto.PackageUpdateDTO;
import com.platform.pkg.vo.PackageVO;

import java.util.List;

/**
 * 套餐管理 Service
 */
public interface PackageService {

    /**
     * 创建套餐
     */
    PackageVO create(PackageCreateDTO dto);

    /**
     * 更新套餐
     */
    PackageVO update(Long id, PackageUpdateDTO dto);

    /**
     * 删除套餐（软删除）
     */
    void delete(Long id);

    /**
     * 根据ID获取套餐
     */
    PackageVO getById(Long id);

    /**
     * 分页查询套餐
     */
    PageResult<PackageVO> page(PackagePageQueryDTO dto);

    /**
     * 获取全部在售套餐（status=ENABLED）
     */
    List<PackageVO> listActive();

    /**
     * 上架套餐
     */
    void enable(Long id);

    /**
     * 下架套餐
     */
    void disable(Long id);
}
