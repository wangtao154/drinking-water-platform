package com.platform.iot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("command_log")
public class CommandLog extends BaseEntity {

    private String messageId;

    private String sn;

    private String deviceId;

    private String pointId;

    private String value;

    private String status;

    /** 指令类型：SET（配置下发）/ HEARTBEAT（心跳检测） */
    private String commandType;

    private Long operatorId;

    private LocalDateTime ackReceivedAt;
}
