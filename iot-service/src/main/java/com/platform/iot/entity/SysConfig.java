package com.platform.iot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体（与 system-service 共享同一张 sys_config 表）
 * iot-service 仅用于读取 mqtt.* 配置项
 */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String configKey;

    private String configValue;

    private String configDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
