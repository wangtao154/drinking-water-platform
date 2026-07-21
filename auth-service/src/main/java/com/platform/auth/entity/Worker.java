package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运维人员实体（auth-service 内部使用，仅查询所需字段）
 * 对应 user-service 管理的 worker 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("worker")
public class Worker extends BaseEntity {

    private String name;
    private String phone;
    private String openId;
    private String password;
    private Boolean passwordChanged;
    private String province;
    private String city;
    private String district;
    private String address;
    private Long dealerId;
    private String status;
    private Integer serviceCount;
    private BigDecimal rating;
    private LocalDateTime registeredAt;
}
