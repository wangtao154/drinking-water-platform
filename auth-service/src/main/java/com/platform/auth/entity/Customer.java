package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 客户实体（auth-service 内部使用，仅查询所需字段）
 * 对应 user-service 管理的 customer 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {

    private String customerType;
    private String name;
    private String phone;
    private String password;
    private Boolean passwordChanged;
    private String wechat;
    private String orgName;
    private String creditCode;
    private String contactName;
    private String contactPhone;
    private String contactWechat;
    private String bankAccount;
    private Long balance;
    private Integer remainDuration;
    private Long remainFlow;
    private String province;
    private String city;
    private String district;
    private String address;
    private String openId;
    private String unionId;
    private Long dealerId;
    private String status;
    private LocalDateTime registeredAt;
}
