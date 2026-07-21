package com.platform.iot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("device")
public class Device {

    private Long id;

    private String deviceId;

    private String sn;

    private Long modelId;

    private Integer onlineStatus;

    private String lifecycleStatus;
}
