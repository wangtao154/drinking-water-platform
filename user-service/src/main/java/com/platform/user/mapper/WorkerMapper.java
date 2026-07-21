package com.platform.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.user.entity.Worker;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkerMapper extends BaseMapper<Worker> {

    default Worker selectByPhone(String phone) {
        return selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getPhone, phone));
    }

    default Worker selectByOpenId(String openId) {
        return selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getOpenId, openId));
    }
}
