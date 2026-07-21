package com.platform.monitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    /**
     * 主键ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备SN
     */
    private String sn;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 告警级别：WARNING / ALARM
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
    private String relatedFilterId;

    /**
     * 触发时间
     */
    private LocalDateTime triggeredAt;

    /**
     * 推送时间
     */
    private LocalDateTime pushedAt;

    /**
     * 处理时间
     */
    private LocalDateTime handledAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
