package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.auth.entity.Worker;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运维人员表 Mapper（auth-service 内部直接查询 worker 表）
 */
@Mapper
public interface WorkerMapper extends BaseMapper<Worker> {

    /**
     * 根据手机号查询运维人员
     */
    default Worker selectByPhone(String phone) {
        return this.selectOne(
                Wrappers.<Worker>lambdaQuery()
                        .eq(Worker::getPhone, phone)
                        .last("LIMIT 1")
        );
    }

    /**
     * 根据微信 openId 查询运维人员
     */
    default Worker selectByOpenId(String openId) {
        return this.selectOne(
                Wrappers.<Worker>lambdaQuery()
                        .eq(Worker::getOpenId, openId)
                        .last("LIMIT 1")
        );
    }
}
