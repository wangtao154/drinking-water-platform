package com.platform.common.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 通用 Mapper 基类，扩展 MyBatis-Plus BaseMapper
 * 子模块的 Mapper 继承此接口即可获得通用 CRUD 能力
 *
 * @param <T> 实体类型
 */
public interface BaseMapperPlus<T> extends BaseMapper<T> {
    // 扩展方法可在各服务中按需添加
}
