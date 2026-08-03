package com.platform.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.iot.entity.CommandLog;
import org.apache.ibatis.annotations.Select;

public interface CommandLogMapper extends BaseMapper<CommandLog> {

    @Select("SELECT * FROM command_log WHERE message_id = #{messageId} LIMIT 1")
    CommandLog selectByMessageId(String messageId);
}
