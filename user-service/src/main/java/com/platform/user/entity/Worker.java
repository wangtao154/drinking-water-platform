package com.platform.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
