package com.platform.iot.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.iot.entity.CommandLog;
import com.platform.iot.mapper.CommandLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 心跳日志定期清理任务
 *
 * 每天凌晨 3 点清理 command_log 表中超过 7 天的心跳日志（command_type=HEARTBEAT）
 * 避免表膨胀影响查询性能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatLogCleanupJob {

    private final CommandLogMapper commandLogMapper;

    /**
     * 每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldHeartbeatLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        LambdaQueryWrapper<CommandLog> query = new LambdaQueryWrapper<>();
        query.eq(CommandLog::getCommandType, "HEARTBEAT")
             .lt(CommandLog::getCreatedAt, cutoff);

        int deleted = commandLogMapper.delete(query);
        log.info("[HeartbeatLogCleanup] 清理 {} 天前的心跳日志: 删除 {} 条", 7, deleted);
    }
}