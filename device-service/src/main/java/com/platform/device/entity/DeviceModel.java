package com.platform.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_model")
public class DeviceModel extends BaseEntity {

    private String modelCode;

    private String modelName;

    private String category;

    private String description;

    private String status;

    private String filterConfig;
}
