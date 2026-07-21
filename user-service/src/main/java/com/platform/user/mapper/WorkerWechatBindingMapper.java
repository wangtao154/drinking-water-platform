package com.platform.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.user.entity.WorkerWechatBinding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkerWechatBindingMapper extends BaseMapper<WorkerWechatBinding> {

    default WorkerWechatBinding selectByWorkerId(Long workerId) {
        return selectOne(new LambdaQueryWrapper<WorkerWechatBinding>()
                .eq(WorkerWechatBinding::getWorkerId, workerId));
    }

    default WorkerWechatBinding selectByOfficialOpenId(String officialOpenId) {
        return selectOne(new LambdaQueryWrapper<WorkerWechatBinding>()
                .eq(WorkerWechatBinding::getOfficialOpenId, officialOpenId));
    }
}
