package com.platform.push.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备告警实体
 */
@Data
@TableName("device_alert")
public class DeviceAlert implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备序列号
     */
    private String sn;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 告警级别
     */
    private String alertLevel;

    /**
     * 告警消息
     */
    private String alertMessage;

    /**
     * 推送状态：UNPUSHED / PUSHED
     */
    private String pushStatus;

    /**
     * 处理状态：UNHANDLED / HANDLED
     */
    private String handledStatus;

    /**
     * 自动生成的工单ID
     */
    private Long autoWorkOrderId;

    /**
     * 关联滤芯ID
     */
    private Long relatedFilterId;

    /**
     * 触发时间
     */
    private LocalDateTime triggeredAt;

    /**
     * 推送时间
     */
    private LocalDateTime pushedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
